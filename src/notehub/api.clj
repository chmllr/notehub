(ns notehub.api
  (:import
   [java.util Calendar])
  (:use
   [notehub.settings]
   [ring.util.codec :only [url-encode]]
   [clojure.string :rename {replace sreplace}
    :only [replace blank? trim lower-case split-lines split]])
  (:require
   [ring.util.codec]
   [hiccup.util :as util]
   [notehub.storage :as storage]))

(def version "1.3")

(def domain
  (get-setting
   (if (get-setting :dev-mode)
     :dev-domain
     :prod-domain)))

(defn log
  "Logs args to the server stdout"
  [string & args]
  (apply printf (str "%s:" string) (str (storage/get-current-date) ":LOG") args)
  (println))

(defn url
  "Creates a local url from the given substrings"
  [& args]
  (apply str (interpose "/" (cons "" (map url-encode args)))))

; Concatenates all fields to a string
(defn build-key
  "Returns a storage-key for the given note coordinates"
  [year month day title]
  (apply str (interpose "/" [year month day title])))

(defn derive-title [md-text]
   (sreplace (first (split-lines md-text))
             #"(#+|_|\*+|<.*?>)" ""))

(defn get-date
  "Returns today's date"
  []
  (map #(+ (second %) (.get (Calendar/getInstance) (first %)))
       {Calendar/YEAR 0, Calendar/MONTH 1, Calendar/DAY_OF_MONTH 0}))

(defn- create-response
  ([success] {:success success})
  ([success message & params]
   (assoc (create-response success) :message (apply format message params))))

(defn- get-path [token & [description]]
  (if (= :url description)
    (str domain "/" token)
    (let [[year month day title] (split token #"/")]
      (if description
        (str domain "/" (storage/create-short-url token {:year year :month month :day day :title title}))
        (str domain (url year month day title))))))

(defn version-manager [f params]
  (if-let [version (:version params)]
    (if (and (:noteID params) (< (Double/parseDouble version) 1.3))
      (let [resp (f (assoc params
                           :noteID (sreplace (params :noteID) #" " "/")
                           :noteID* (params :noteID)))
            server-message (get-in resp [:status :message])]
        (assoc-in resp [:status :message]
                  (str 
                    server-message
                    (when server-message "; ") 
                    "this API version is deprecated and "
                    "will be disabled by the end of June 2014!")))
      (f params))
    {:status (create-response false "API version expected")}))

(defn get-note [{:keys [noteID]}]
  (if (storage/note-exists? noteID)
    (let [note (storage/get-note noteID)]
      {:note note
       :title (derive-title note)
       :longURL (get-path noteID)
       :shortURL (get-path noteID :id)
       :statistics (storage/get-note-statistics noteID)
       :status (create-response true)
       :publisher (storage/get-publisher noteID)})
    {:status (create-response false "noteID '%s' unknown" noteID)}))

(defn post-note
  [{:keys [note pid signature password] :as params}]
   ;(log "post-note: %s" {:pid pid :signature signature :password password :note note})
   (let [errors (filter identity
                        [(when-not (storage/valid-publisher? pid) "pid invalid")
                         (when-not (= signature (storage/sign pid (storage/get-psk pid) note))
                           "signature invalid")
                         (when (blank? note) "note is empty")])]
     (if (empty? errors)
       (let [[year month day] (map str (get-date))
             params (dissoc params :note :pid :signature :password)
             raw-title (filter #(or (= \- %) (Character/isLetterOrDigit %))
                               (-> note derive-title trim (sreplace " " "-") lower-case))
             max-length (get-setting :max-title-length #(Integer/parseInt %) 80)
             proposed-title (apply str (take max-length raw-title))
             title (first (drop-while #(storage/note-exists? (build-key year month day %))
                                      (cons proposed-title
                                            (map #(str proposed-title "-" (+ 2 %)) (range)))))
             noteID (build-key year month day title)
             new-params (assoc params :year year :month month :day day :title title)
             short-url (get-path (storage/create-short-url noteID new-params) :url)
             long-url (get-path noteID)]
         (do
           (storage/add-note noteID note pid password)
           {:noteID noteID
            :longURL (if (empty? params) long-url (str (util/url long-url params)))
            :shortURL short-url
            :status (create-response true)}))
       {:status (create-response false (first errors))})))


(defn update-note [{:keys [noteID note pid signature password noteID*]}]
  ;(log "update-note: %s" {:pid pid :noteID noteID :signature signature :password password :note note})
  (let [errors (filter identity
                         [(when-not (storage/valid-publisher? pid) "pid invalid")
                          ; TODO: noteID* is a hack introduced by backwards-comp. to older APIs
                          (when-not (= signature (storage/sign pid (storage/get-psk pid) (or noteID* noteID) note password))
                            "signature invalid")
                          (when (blank? note) "note is empty")
                          (when-not (storage/valid-password? noteID password) "password invalid")])]
    (if (empty? errors)
      (do
        (storage/edit-note noteID note)
        {:longURL (get-path noteID)
         :shortURL (get-path noteID :id)
         :status (create-response true)})
      {:status (create-response false (first errors))})))
