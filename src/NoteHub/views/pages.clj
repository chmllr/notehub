(ns NoteHub.views.pages
  (:require [hiccup.util :as util]
            [NoteHub.api :as api]
            [NoteHub.storage :as storage]
            [cheshire.core :refer :all])
  (:use
    [NoteHub.settings]
    [NoteHub.views.common]
    [clojure.string :rename {replace sreplace}
     :only [escape split replace blank? split-lines lower-case]]
    [clojure.core.incubator :only [-?>]]
    [noir.util.crypt :only [encrypt]]
    [hiccup.form]
    [hiccup.core]
    [hiccup.element]
    [noir.response :only [redirect status content-type]]
    [noir.core :only [defpage defpartial]]
    [noir.statuses]))

(when-not (storage/valid-publisher? api/domain)
  (storage/register-publisher api/domain))

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

; Converts given markdown to html and wraps with the main layout
(defn- wrap [short-url params md-text]
  (when md-text 
    (layout params (params :title)
            [:article.bottom-space.markdown md-text]
            (let [links (map #(link-to 
                                (if (= :short-url %)
                                  (url short-url)
                                  (str (params :title) "/" (name %)))
                                (get-message %))
                             [:stats :edit :export :short-url])
                  space (apply str (repeat 4 "&nbsp;"))
                  separator (str space "&middot;" space)
                  links (interpose separator links)]
              [:div#panel (map identity links)]))))

; input form for the markdown text with a preview area
(defpartial input-form [form-url command fields content passwd-msg]
  (let [css-class (when (= :publish command) :hidden)]
    (layout {:js true} (get-message :new-note)
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
(defpage "/:year/:month/:day/:title" {:keys [year month day title theme header-font text-font] :as params}
  (wrap 
    (storage/create-short-url params)
    (select-keys params [:title :theme :header-font :text-font])
    (:note (api/get-note (api/build-key [year month day] title)))))

; Provides Markdown of the specified note
(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
  (when-let [md-text (:note (api/get-note (api/build-key [year month day] title)))]
    (content-type "text/plain; charset=utf-8" md-text)))

; Provides the number of views of the specified note
(defpage "/:year/:month/:day/:title/stats" {:keys [year month day title]}
  (when-let [stats (:statistics (api/get-note (api/build-key [year month day] title)))]
    (layout (get-message :statistics)
            [:table#stats.helvetica.central-element
             [:tr
              [:td (get-message :published)]
              [:td (:published stats)]]
             (when (:edited stats)
               [:tr
                [:td (get-message :edited)]
                [:td (:edited stats)]])
             [:tr
              [:td (get-message :article-views)]
              [:td (:views stats)]]])))

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
    (let [pid api/domain
          psk (storage/get-psk pid)]
      (if (storage/valid-publisher? pid)
        (let [resp (api/post-note note pid (api/get-signature (str pid psk note)) password)]
          (if (get-in resp [:status :success])
            (redirect (:longPath resp))
            (response 400)))
        (response 500)))
    (response 400)))

; Updates a note
(defpage [:post "/update-note"] {:keys [noteID note password version]}
  (let [pid api/domain
        psk (storage/get-psk pid)]
    (if (storage/valid-publisher? pid)
      (let [resp (api/update-note noteID note pid 
                                      (api/get-signature (str pid psk noteID note password)) 
                                      password)]
        (if (get-in resp [:status :success])
          (redirect (:longPath resp))
          (response 403)))
      (response 500))))

; Here lives the API

(defpage "/api" args
  (let [title (get-message :api-title)]
  (layout title [:article.markdown (slurp "API.md")])))

(defpage [:get "/api/note"] {:keys [version noteID]}
  (generate-string (api/get-note noteID)))

(defpage [:post "/api/note"] {:keys [version note pid signature password]}
  (generate-string (api/post-note note pid signature password)))

(defpage [:put "/api/note"] {:keys [version noteID note pid signature password]}
  (generate-string (api/update-note noteID note pid signature password)))

