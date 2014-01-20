(ns NoteHub.views.pages
  (:require [hiccup.util :as util]
            [NoteHub.api :as api]
            [NoteHub.storage :as storage]
            [cheshire.core :refer :all])
  (:use
   [NoteHub.settings]
   [clojure.string :rename {replace sreplace}
    :only [escape split replace blank? split-lines lower-case]]
   [clojure.core.incubator :only [-?>]]
   [noir.util.crypt :only [encrypt]]
   [hiccup.form]
   [hiccup.core]
   [ring.util.codec :only [url-encode]]
   [hiccup.element]
   [hiccup.util :only [escape-html]]
   [hiccup.page :only [include-js html5]]
   [noir.response :only [redirect status content-type]]
   [noir.core :only [defpage defpartial]]
   [noir.statuses]))

(when-not (storage/valid-publisher? "NoteHub")
  (storage/register-publisher "NoteHub"))

; Creates the main html layout
(defpartial layout
  [title & content]
  (html5
   [:head
    [:title (print-str (get-message :name) "&mdash;" title)]
    [:meta {:charset "UTF-8"}]
    [:link {:rel "stylesheet/less" :type "text/css" :href "/style.less"}]
    (html
     (include-js "/js/less.js")
     (include-js "/js/md5.js")
     (include-js "/js/marked.js")
     (include-js "/js/main.js")
     (include-js "/js/themes.js"))
    ; google analytics code should appear in prod mode only
    (if-not (get-setting :dev-mode?) (include-js "/js/google-analytics.js"))]
   [:body {:onload "onLoad()"} content]))

; Sets a custom message for each needed HTTP status.
; The message to be assigned is extracted with a dynamically generated key
(doseq [code [400 403 404 500]]
  (set-page! code
             (let [message (get-message (keyword (str "status-" code)))]
               (layout message
                       [:article [:h1 message]]))))

; shortcut for rendering an HTTP status
(defn- response [code]
  (status code (get-page code)))

(defn url
  "Creates a local url from the given substrings"
  [& args]
  (apply str (interpose "/" (cons "" (map url-encode args)))))

; input form for the markdown text with a preview area
(defpartial input-form [form-url command fields content passwd-msg]
  (let [css-class (when (= :publish command) :hidden)]
    (layout (get-message :new-note)
            [:article#preview.markdown " "]
            [:div#dashed-line {:class css-class}]
            [:div.central-element.helvetica {:style "margin-bottom: 3em"}
             (form-to {:autocomplete :off} [:post form-url]
                      (hidden-field :action command)
                      (hidden-field :version api/version)
                      (hidden-field :password)
                      fields
                      (text-area {:class :max-width} :note content)
                      [:fieldset#input-elems {:class css-class}
                       (text-field {:class "ui-elem" :placeholder (get-message passwd-msg)}
                                   :plain-password)
                       (submit-button {:class "button ui-elem"
                                       :id :publish-button} (get-message command))])])))

(defn generate-session []
  (encrypt (str (rand-int Integer/MAX_VALUE))))

; Routes
; ======

; Landing Page
(defpage "/" {}
  (layout (get-message :page-title)
          [:div#hero
           [:h1 (get-message :name)]
           [:h2 (get-message :title)]
           [:br]
           [:a.landing-button {:href "/new" :style "color: white"} (get-message :new-page)]]
          [:div#dashed-line]
          [:article.helvetica.bottom-space.markdown {:style "font-size: 1em"}
           (slurp "LANDING.md")]
          [:div.centered.helvetica.markdown (get-message :footer)]))

; Displays the note
(defpage "/:year/:month/:day/:title" {:keys [year month day title] :as params}
  (let [noteID (api/build-key [year month day] title)]
    (when (storage/note-exists? noteID)
      (let [note (api/get-note noteID)]
        (layout (:title note)
                [:article.bottom-space.markdown (:note note)]
                (let [links (map #(link-to
                                   (if (= :short-url %)
                                     (url (storage/create-short-url params))
                                     (str (:longURL note) "/" (name %)))
                                   (get-message %))
                                 [:stats :edit :export :short-url])
                      links (interpose [:span.middot "&middot;"] links)]
                  [:div#panel (map identity links)]))))))

; Provides Markdown of the specified note
(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
  (when-let [md-text (:note (api/get-note (api/build-key [year month day] title)))]
    (content-type "text/plain; charset=utf-8" md-text)))

; Provides the number of views of the specified note
(defpage "/:year/:month/:day/:title/stats" {:keys [year month day title]}
  (when-let [stats (:statistics (api/get-note (api/build-key [year month day] title)))]
    (layout (get-message :statistics)
            [:table#stats.helvetica.central-element
             (map
              #(when (% stats)
                 [:tr [:td (str (get-message %) ":")] [:td (% stats)]])
              [:published :edited :publisher :views])])))

; Resolving of a short url
(defpage "/:short-url" {:keys [short-url]}
  (when-let [params (storage/resolve-url short-url)]
    (let [{:keys [year month day title]} params
          rest-params (dissoc params :year :month :day :title)
          core-url (url year month day title)
          long-url (if (empty? rest-params) core-url (util/url core-url rest-params))]
      (redirect long-url))))

; New Note Page
(defpage "/new" {}
  (input-form "/post-note" :publish
              (html (hidden-field :session (storage/create-session))
                    (hidden-field {:id :signature} :signature))
              (get-message :loading) :set-passwd))

; Update Note Page
(defpage "/:year/:month/:day/:title/edit" {:keys [year month day title]}
  (let [noteID (api/build-key [year month day] title)]
    (input-form "/update-note" :update
                (html (hidden-field :noteID noteID))
                (:note (api/get-note noteID)) :enter-passwd)))

; Creates New Note from Web
(defpage [:post "/post-note"] {:keys [session note signature password version]}
  (if (= signature (api/get-signature session note))
    (let [pid "NoteHub"
          psk (storage/get-psk pid)]
      (if (storage/valid-publisher? pid)
        (let [resp (api/post-note note pid (api/get-signature pid psk note) password)]
          (if (and
               (storage/invalidate-session session)
               (get-in resp [:status :success]))
            (redirect (:longURL resp))
            (response 400)))
        (response 500)))
    (response 400)))

; Updates a note
(defpage [:post "/update-note"] {:keys [noteID note password version]}
  (let [pid "NoteHub"
        psk (storage/get-psk pid)]
    (if (storage/valid-publisher? pid)
      (let [resp (api/update-note noteID note pid
                                  (api/get-signature pid psk noteID note password)
                                  password)]
        (if (get-in resp [:status :success])
          (redirect (:longURL resp))
          (response 403)))
      (response 500))))

; Here lives the API

(defpage "/api" args
    (layout (get-message :api-title)
            [:article.markdown (slurp "API.md")]))

(defpage [:get "/api/note"] {:keys [version noteID]}
  (generate-string (api/get-note noteID)))

(defpage [:post "/api/note"] {:keys [version note pid signature password]}
  (generate-string (api/post-note note pid signature password)))

(defpage [:put "/api/note"] {:keys [version noteID note pid signature password]}
  (generate-string (api/update-note noteID note pid signature password)))
