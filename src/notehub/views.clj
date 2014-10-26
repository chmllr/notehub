(ns notehub.views
  (:use
    iokv.core
    [clojure.string :rename {replace sreplace} :only [replace]]
    [hiccup.form]
    [hiccup.core]
    [hiccup.element]
    [hiccup.util :only [escape-html]]
    [hiccup.page :only [include-js html5]])
  (:import (org.pegdown PegDownProcessor Extensions)))

(def get-message (get-map "messages"))

(def md-processor
  (PegDownProcessor. (int (bit-and-not Extensions/ALL Extensions/HARDWRAPS))))

(defn md-to-html [md-text]
  (.markdownToHtml md-processor md-text))

; Creates the main html layout
(defn layout
  [js? title & content]
  (html5
    [:head
     [:title (print-str (get-message :name) "&mdash;" title)]
     [:meta {:charset "UTF-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
     [:link {:rel "stylesheet/less" :type "text/css" :href "/styles/main.less"}]
     (html
       (include-js "/js/less.js")
       (include-js "/js/themes.js"))
     (when (= :js js?)
       (html
         (include-js "/js/md5.js")
         (include-js "/js/marked.js")
         (include-js "/js/main.js")))]
    [:body {:onload "onLoad()"} content]))

(defn- sanitize
  "Breakes all usages of <script> & <iframe>"
  [input]
  (sreplace input #"(</?(iframe|script).*?>|javascript:)" ""))

; input form for the markdown text with a preview area
(defn- input-form [form-url command fields content passwd-msg]
  (let [css-class (when (= :publish command) :hidden)]
    (layout :js (get-message :new-page)
            [:article#preview ""]
            [:div#dashed-line {:class css-class}]
            [:div.central-element.helvetica {:style "margin-bottom: 3em"}
             (form-to {:autocomplete :off} [:post form-url]
                      (hidden-field :action command)
                      (hidden-field :password)
                      fields
                      (text-area {:class :max-width} :note content)
                      [:fieldset#input-elems {:class css-class}
                       (text-field {:class "ui-elem" :placeholder (get-message passwd-msg)}
                                   :plain-password)
                       (submit-button {:class "button ui-elem"
                                       :id :publish-button} (get-message command))])])))

(def landing-page
  (layout :no-js (get-message :page-title)
          [:div#hero
           [:h1 (get-message :name)]
           [:h2 (get-message :title)]
           [:br]
           [:a.landing-button {:href "/new" :style "color: white"} (get-message :new-page)]]
          [:div#dashed-line]
          [:article.helvetica.bottom-space
           {:style "font-size: 1em"}
           (md-to-html (slurp "LANDING.md"))]
          [:div#footer (md-to-html (get-message :footer))]))


(defn statistics-page [note-title stats publisher]
  (let [page-title (get-message :statistics)
        info (assoc stats :publisher publisher)]
    (layout :no-js page-title
            [:h2.central-element note-title]
            [:h3.central-element.helvetica page-title]
            [:table#stats.helvetica.central-element
             (map
               #(when-let [v (% info)]
                 [:tr
                  [:td (str (get-message %) ":")]
                  [:td (if (or (= % :published) (= % :edited))
                         (str (java.util.Date. (Long/parseLong v))) v)]])
               [:published :edited :publisher :views])])))

(defn note-update-page [note-id note]
  (input-form "/update-note"
              :update
              (html (hidden-field :noteID note-id))
              note
              :enter-passwd))

(defn new-note-page [session]
  (input-form "/post-note" :publish
              (html (hidden-field :session session)
                    (hidden-field {:id :signature} :signature))
              (get-message :loading) :set-passwd))

(defn note-page [note short-url]
  (layout :no-js (:title note)
          [:article.bottom-space (md-to-html (sanitize (:note note)))]
          (let [urls {:short-url short-url
                      :notehub "/"}
                links (map #(link-to
                             (urls % (str (:longURL note) "/" (name %)))
                             (get-message %))
                           [:notehub :stats :edit :export :short-url])]
            [:div#footer (interpose [:span.middot "&middot;"] links)])))
