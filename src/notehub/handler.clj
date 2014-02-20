(ns notehub.handler
  (:use compojure.core
        [notehub.settings]
        [clojure.string :rename {replace sreplace} :only [replace]]
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

(when-not (storage/valid-publisher? "NoteHub")
  (storage/register-publisher "NoteHub"))

(defn sanitize
  "Breakes all usages of <script> & <iframe>"
  [input]
  (sreplace input #"(</?(iframe|script).*?>|javascript:)" ""))

; input form for the markdown text with a preview area
(defn input-form [form-url command fields content passwd-msg]
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

; TODO: make sure the status is really set to the response!!!!
(defn- response
  "Sets a custom message for each needed HTTP status.
  The message to be assigned is extracted with a dynamically generated key"
  [code]
  {:status code
   :body (let [message (get-message (keyword (str "status-" code)))]
           (layout message
                   [:article [:h1 message]]))})

(defn redirect [url]
  {:status 302
   :headers {"Location" (str url)}
   :body ""})

(defn return-content-type [ctype content]
  {:headers {"Content-Type" ctype}
   :body content})

(defroutes api-routes
  (GET "/note" {params :params}
       (generate-string (api/version-manager api/get-note params)))

  (POST "/note" {params :params}
        (generate-string (api/version-manager api/post-note params)))

  (PUT "/note" {params :params}
       (generate-string (api/version-manager api/update-note params))))

(defroutes app-routes
  (GET "/api" [] (layout (get-message :api-title)
                      (md-node :article (slurp "API.md"))))

  (context "/api" []
           #(ring.util.response/content-type (api-routes %) "application/json"))

  (GET "/" []
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

  (GET "/:year/:month/:day/:title/export" [year month day title]
       (when-let [md-text (:note (api/get-note {:noteID (api/build-key year month day title)}))]
         (return-content-type "text/plain; charset=utf-8" md-text)))

  (GET "/:year/:month/:day/:title/stats" [year month day title]
       (when-let [resp (api/get-note {:noteID (api/build-key year month day title)})]
         (let [stats (:statistics resp)
               statistics (get-message :statistics)]
           (layout statistics
                   [:h2.central-element (api/derive-title (:note resp))]
                   [:h3.central-element.helvetica statistics]
                   [:table#stats.helvetica.central-element
                    (map
                     #(when (% stats)
                        [:tr [:td (str (get-message %) ":")] [:td (% stats)]])
                     [:published :edited :publisher :views])]))))

  (GET "/:year/:month/:day/:title/edit" [year month day title]
       (let [noteID (api/build-key year month day title)]
         (input-form "/update-note" :update
                     (html (hidden-field :noteID noteID))
                     (:note (api/get-note {:noteID noteID})) :enter-passwd)))

  (GET "/new" []
       (input-form "/post-note" :publish
                   (html (hidden-field :session (storage/sign (str (rand-int Integer/MAX_VALUE))))
                         (hidden-field {:id :signature} :signature))
                   (get-message :loading) :set-passwd))

  (GET "/:year/:month/:day/:title" [year month day title :as params]
       (let [params (assoc (:query-params params)
                      :year year :month month :day day :title title)
             noteID (api/build-key year month day title)]
         (when (storage/note-exists? noteID)
           (let [note (api/get-note {:noteID noteID})
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

  (GET "/:short-url" [short-url]
       (when-let [params (storage/resolve-url short-url)]
         (let [{:keys [year month day title]} params
               rest-params (dissoc params :year :month :day :title)
               core-url (api/url year month day title)
               long-url (if (empty? rest-params) core-url (util/url core-url rest-params))]
           (redirect long-url))))


  (POST "/post-note" [session note signature password]
        (if (= signature (storage/sign session note))
          (let [pid "NoteHub"
                psk (storage/get-psk pid)
                params {:session session :note note :signature signature
                        :password password :pid pid}]
            (if (storage/valid-publisher? pid)
              (let [resp (api/post-note (assoc params :signature (storage/sign pid psk note)))]
                (if (get-in resp [:status :success])
                  (redirect (:longURL resp))
                  (response 400)))
              (response 500)))
          (response 400)))

  (POST "/update-note" [noteID note password]
        (let [pid "NoteHub"
              psk (storage/get-psk pid)
              params {:noteID noteID :note note :password password :pid pid}]
          (if (storage/valid-publisher? pid)
            (let [resp (api/update-note (assoc params
                                          :signature
                                          (storage/sign pid psk noteID note password)))]
              (if (get-in resp [:status :success])
                (redirect (:longURL resp))
                (response 403)))
            (response 500))))

  (route/resources "/")
  (route/not-found (response 404)))

(def app
  (let [handler (handler/site app-routes)]
     (fn [request]
       (if (get-setting :dev-mode)
         (handler request)
         (try (handler request)
           (catch Exception e
             (do
               ;TODO (log e)
               (response 500))))))))
