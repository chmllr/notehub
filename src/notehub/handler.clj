(ns notehub.handler
  (:use 
    compojure.core
    iokv.core
    notehub.views
    [clojure.string :rename {replace sreplace} :only [replace]]
    [clojure.core.incubator :only [-?>]])
  (:require 
    [me.raynes.cegdown :as md]
    [clojure.core.cache :as cache]
    [hiccup.util :as util]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [notehub.api :as api]
    [notehub.storage :as storage]
    [cheshire.core :refer :all]))

; note page cache
(def C (atom (cache/lru-cache-factory {})))

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
                      [:article (md/to-html (slurp "API.md"))]))

  (context "/api" []
           #(ring.util.response/content-type (api-routes %) "application/json"))

  (GET "/" [] landing-page)

  (GET "/:year/:month/:day/:title/export" [year month day title]
       (when-let [md-text (:note (api/get-note {:noteID (api/build-key year month day title)}))]
         (return-content-type "text/plain; charset=utf-8" md-text)))

  (GET "/:year/:month/:day/:title/stats" [year month day title]
       (let [note-id (api/build-key year month day title)
             resp {:statistics (storage/get-note-statistics note-id)
                   :note (storage/get-note note-id)
                   :noteID note-id}]
         (statistics-page resp)))

  (GET "/:year/:month/:day/:title/edit" [year month day title]
       (note-update-page year month day title))

  (GET "/new" [] (new-note-page (storage/sign (str (rand-int Integer/MAX_VALUE)))))

  (GET "/:year/:month/:day/:title" [year month day title :as params]
       (let [params (assoc (:query-params params)
                           :year year :month month :day day :title title)
             note-id (api/build-key year month day title)
             short-url (storage/create-short-url note-id params)]
         (when (storage/note-exists? note-id)
           (if (cache/has? @C short-url)
             (do
               (swap! C cache/hit short-url)
               (storage/increment-note-view note-id))
             (swap! C cache/miss short-url
                    (note-page note-id short-url)))
           (cache/lookup @C short-url))))

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
                (do
                  (doseq [url (storage/get-short-urls noteID)]
                    (swap! C cache/evict url))
                  (redirect (:longURL resp)))
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
