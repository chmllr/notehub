(ns notehub.handler
  (:use compojure.core
        [notehub.settings]
        [clojure.string :rename {replace sreplace}
         :only [escape split replace blank? split-lines lower-case]]
        [clojure.core.incubator :only [-?>]]
        [hiccup.form]
        [hiccup.core]
        [hiccup.element]
        [hiccup.util :only [escape-html]]
        [hiccup.page :only [include-js html5]])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [hiccup.util :as util]
            [notehub.api :as api]
            [notehub.storage :as storage]
            [cheshire.core :refer :all]))


; Creates the main html layout
(defn layout
  [title & content]
  (html5
   [:head
    [:title (print-str (get-message :name) "&mdash;" title)]
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet/less" :type "text/css" :href "/styles/main.less"}]
    (html
     (include-js "/js/less.js")
     (include-js "/js/themes.js")
     (include-js "/js/md5.js")
     (include-js "/js/marked.js")
     (include-js "/js/main.js"))
    ; google analytics code should appear in prod mode only
    (if-not (get-setting :dev-mode) (include-js "/js/google-analytics.js"))]
   [:body {:onload "onLoad()"} content]))


(defn md-node
  "Returns an HTML element with a textarea inside
  containing the markdown text (to keep all chars unescaped)"
  ([cls input] (md-node cls {} input))
  ([cls opts input]
  [(keyword (str (name cls) ".markdown")) opts
   [:textarea input]]))

#_ (

    ; ######## OLD CODE START

(ns NoteHub.views.pages
  (:require )
  (:use


   [noir.response :only [redirect status content-type]]
   [noir.core :only [defpage defpartial]]
   [noir.statuses]
   [noir.util.crypt :only [encrypt]]))

(when-not (storage/valid-publisher? "NoteHub")
  (storage/register-publisher "NoteHub"))

(defn sanitize
  "Breakes all usages of <script> & <iframe>"
  [input]
  (sreplace input #"(</?(iframe|script).*?>|javascript:)" ""))

; Sets a custom message for each needed HTTP status.
; The message to be assigned is extracted with a dynamically generated key
(doseq [code [400 403 404 500]]
  (set-page! code
             (let [message (get-message (keyword (str "status-" code)))]
               (layout message
                       [:article [:h1 message]]))))

(defn- response
  "shortcut for rendering an HTTP status"
  [code]
  (status code (get-page code)))

; input form for the markdown text with a preview area
(defpartial input-form [form-url command fields content passwd-msg]
  (let [css-class (when (= :publish command) :hidden)]
    (layout (get-message :new-note)
            [:article#preview ""]
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

(defpage "/" {}
  (layout (get-message :page-title)
          [:div#hero
           [:h1 (get-message :name)]
           [:h2 (get-message :title)]
           [:br]
           [:a.landing-button {:href "/new" :style "color: white"} (get-message :new-page)]]
          [:div#dashed-line]
          (md-node :article.helvetica.bottom-space
                   {:style "font-size: 1em"}
                   (slurp "LANDING.md"))
          (md-node :div.centered.helvetica (get-message :footer))))

(defpage "/:year/:month/:day/:title" {:keys [year month day title] :as params}
  (let [noteID (api/build-key [year month day] title)]
    (when (storage/note-exists? noteID)
      (let [note (api/get-note noteID)
            sanitized-note (sanitize (:note note))]
        (layout (:title note)
                (md-node :article.bottom-space sanitized-note)
                (let [urls {:short-url (api/url (storage/create-short-url noteID params))
                            :notehub "/"}
                      links (map #(link-to
                                   (if (urls %)
                                     (urls %)
                                     (str (:longURL note) "/" (name %)))
                                   (get-message %))
                                 [:notehub :stats :edit :export :short-url])
                      links (interpose [:span.middot "&middot;"] links)]
                  [:div#links links]))))))

(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
  (when-let [md-text (:note (api/get-note (api/build-key [year month day] title)))]
    (content-type "text/plain; charset=utf-8" md-text)))

(defpage "/:year/:month/:day/:title/stats" {:keys [year month day title]}
  (when-let [stats (:statistics (api/get-note (api/build-key [year month day] title)))]
    (layout (get-message :statistics)
            [:table#stats.helvetica.central-element
             (map
              #(when (% stats)
                 [:tr [:td (str (get-message %) ":")] [:td (% stats)]])
              [:published :edited :publisher :views])])))


(defpage "/:year/:month/:day/:title/edit" {:keys [year month day title]}
  (let [noteID (api/build-key [year month day] title)]
    (input-form "/update-note" :update
                (html (hidden-field :noteID noteID))
                (:note (api/get-note noteID)) :enter-passwd)))

(defpage "/new" {}
  (input-form "/post-note" :publish
              (html (hidden-field :session (storage/create-session))
                    (hidden-field {:id :signature} :signature))
              (get-message :loading) :set-passwd))

(defpage [:post "/post-note"] {:keys [session note signature password version]}
  (if (= signature (api/get-signature session note))
    (let [pid "NoteHub"
          psk (storage/get-psk pid)]
      (if (storage/valid-publisher? pid)
        (let [resp (api/post-note note pid (api/get-signature pid psk note) {:password password})]
          (if (and
               (storage/invalidate-session session)
               (get-in resp [:status :success]))
            (redirect (:longURL resp))
            (response 400)))
        (response 500)))
    (response 400)))

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

; ###### END OLD CODE
)

(defn redirect [url]
      {:status 302
       :headers {"Location" (str url)}
       :body ""})

(defroutes api-routes

  (GET "/" [] (layout (get-message :api-title)
               (md-node :article (slurp "API.md"))))

  (GET "/note" [version noteID]
       (generate-string (api/get-note noteID)))

  (POST "/note" {params :params}
        (generate-string
         (api/post-note
          (:note params)
          (:pid params)
          (:signature params)
          {:params (dissoc params :version :note :pid :signature :password)
           :password (:password params)})))

  (PUT "/note" [version noteID note pid signature password]
       (generate-string (api/update-note noteID note pid signature password))))

(defroutes app-routes
  (context "/api" [] api-routes)
  (GET "/" [] "Hello World")

  (GET "/:short-url" [short-url]
  (when-let [params (storage/resolve-url short-url)]
    (let [{:keys [year month day title]} params
          rest-params (dissoc params :year :month :day :title)
          core-url (api/url year month day title)
          long-url (if (empty? rest-params) core-url (util/url core-url rest-params))]
      (redirect long-url))))

  (route/resources "/resources")
  (route/not-found "Not Found"))

(def app
  (handler/site app-routes))
