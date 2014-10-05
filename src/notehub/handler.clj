(ns notehub.handler
  (:use
    compojure.core
    iokv.core
    notehub.views
    [clojure.string :rename {replace sreplace} :only [replace]])
  (:require
    [ring.adapter.jetty :as jetty]
    [clojure.core.cache :as cache]
    [hiccup.util :as util]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [notehub.api :as api]
    [notehub.storage :as storage]
    [cheshire.core :refer :all])
  (:gen-class))

(defn current-timestamp []
  (quot (System/currentTimeMillis) 100000000))

; note page cache
(def page-cache (atom (cache/lru-cache-factory {})))

; TODO: make sure the status is really set to the response!!!!
(defn- response
  "Sets a custom message for each needed HTTP status.
  The message to be assigned is extracted with a dynamically generated key"
  [code]
  {:status code
   :body (let [message (get-message (keyword (str "status-" code)))]
           (layout :no-js message
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
           (GET "/api" [] (layout :no-js (get-message :api-title)
                                  [:article (md-to-html (slurp "API.md"))]))

           (context "/api" []
                    #(ring.util.response/content-type (api-routes %) "application/json"))

           (GET "/" [] landing-page)

           (GET "/:year/:month/:day/:title/export" [year month day title]
                (when-let [md-text (:note (api/get-note {:noteID (api/build-key year month day title)}))]
                  (return-content-type "text/plain; charset=utf-8" md-text)))

           (GET "/:year/:month/:day/:title/stats" [year month day title]
                (let [note-id (api/build-key year month day title)]
                  (statistics-page (api/derive-title (storage/get-note note-id))
                                   (storage/get-note-statistics note-id)
                                   (storage/get-publisher note-id))))

           (GET "/:year/:month/:day/:title/edit" [year month day title]
                (let [note-id (api/build-key year month day title)]
                  (note-update-page
                    note-id
                    (:note (api/get-note {:noteID note-id})))))

           (GET "/new" [] (new-note-page
                            (str
                              (current-timestamp)
                              (storage/sign (rand-int Integer/MAX_VALUE)))))

           (GET "/:year/:month/:day/:title" [year month day title :as params]
                (let [params (assoc (:query-params params)
                               :year year :month month :day day :title title)
                      note-id (api/build-key year month day title)
                      short-url (storage/create-short-url note-id params)]
                  (when (storage/note-exists? note-id)
                    (if (cache/has? @page-cache short-url)
                      (do
                        (swap! page-cache cache/hit short-url)
                        (storage/increment-note-view note-id))
                      (swap! page-cache cache/miss short-url
                             (note-page (api/get-note {:noteID note-id})
                                        (api/url short-url))))
                    (cache/lookup @page-cache short-url))))

           (GET "/:short-url" [short-url]
                (when-let [params (storage/resolve-url short-url)]
                  (let [{:keys [year month day title]} params
                        rest-params (dissoc params :year :month :day :title)
                        core-url (api/url year month day title)
                        long-url (if (empty? rest-params) core-url (util/url core-url rest-params))]
                    (redirect long-url))))

           (POST "/post-note" [session note signature password]
                 (if (and session
                          (.startsWith session
                                       (str (current-timestamp)))
                          (= signature (storage/sign session note)))
                   (let [pid "NoteHub"
                         psk (storage/get-psk pid)
                         params {:note note
                                 :pid pid
                                 :signature (storage/sign pid psk note)
                                 :password password}]
                     (if (storage/valid-publisher? pid)
                       (let [resp (api/post-note params)]
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
                             (swap! page-cache cache/evict url))
                           (redirect (:longURL resp)))
                         (response 403)))
                     (response 500))))

           (route/resources "/")
           (route/not-found (response 404)))

(def app
  (let [handler (handler/site app-routes)]
    (fn [request]
      (let [{:keys [server-name server-port]} request
            hostURL (str "https://" server-name
                         (when (not= 80 server-port) (str ":" server-port)))
            request (assoc-in request [:params :hostURL] hostURL)]
        (if (get-setting :dev-mode)
          (handler request)
          (try (handler request)
               (catch Exception e
                 (do
                   ;TODO (log e)
                   (response 500)))))))))

(defn -main [& [port]]
  (jetty/run-jetty #'app
                   {:port (if port (Integer/parseInt port) 8080)}))

