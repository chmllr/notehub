(ns notehub.storage
  (:use [iokv.core]
        [zeus.core]
        [clojure.string :only (blank? replace) :rename {replace sreplace}])
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def conn {:pool {} :spec {:uri (get-setting :db-url)}})

(let [md5Instance (java.security.MessageDigest/getInstance "MD5")]
  (defn sign
    "Returns the MD5 hash for the concatenation of all passed parameters"
    [& args]
    (let [input (sreplace (apply str args) #"\r+" "")]
      (do (.reset md5Instance)
          (.update md5Instance (.getBytes input))
          (apply str
                 (map #(let [c (Integer/toHexString (bit-and 0xff %))]
                        (if (= 1 (count c)) (str "0" c) c))
                      (.digest md5Instance)))))))

(defmacro redis [cmd & body]
  `(car/wcar conn
             (~(symbol "car" (name cmd))
              ~@body)))

(defn get-current-date []
  (.getTime (java.util.Date.)))

(defn valid-publisher? [pid]
  (= 1 (redis :hexists :publisher-key pid)))

(defn register-publisher [pid]
  "Returns nil if given PID exists or a PSK otherwise"
  (when (not (valid-publisher? pid))
    (let [psk (sign (str (rand-int Integer/MAX_VALUE) pid))]
      (redis :hset :publisher-key pid psk)
      psk)))

(when (and (get-setting :dev-mode)
           (not (valid-publisher? "NoteHub"))
           (register-publisher "NoteHub")))

(defn revoke-publisher [pid]
  (redis :hdel :publisher-key pid))

(defn get-psk [pid]
  (redis :hget :publisher-key pid))

(defn edit-note [noteID text]
  (redis :hset :edited noteID (get-current-date))
  (redis :hset :note noteID (zip text)))

(defn add-note
  ([noteID text pid] (add-note noteID text pid nil))
  ([noteID text pid passwd]
   (redis :hset :note noteID (zip text))
   (redis :hset :published noteID (get-current-date))
   (redis :hset :publisher noteID pid)
   (when (not (blank? passwd))
     (redis :hset :password noteID passwd))))

(defn valid-password? [noteID passwd]
  (let [stored (redis :hget :password noteID)]
    (and (not= 0 stored) (= stored passwd))))

(defn get-note-views [noteID]
  (redis :hget :views noteID))

(defn get-publisher [noteID]
  (redis :hget :publisher noteID))

(defn get-note-statistics [noteID]
  {:views (get-note-views noteID)
   :published (redis :hget :published noteID)
   :edited (redis :hget :edited noteID)})

(defn note-exists? [noteID]
  (= 1 (redis :hexists :note noteID)))

(defn get-note [noteID]
  (when (note-exists? noteID)
    (unzip (redis :hget :note noteID))))

(defn increment-note-view [noteID]
  (redis :hincrby :views noteID 1))

(defn short-url-exists? [url]
  (= 1 (redis :hexists :short-url url)))

(defn resolve-url [url]
  (let [value (redis :hget :short-url url)]
    (when value ; TODO: necessary?
      (read-string value))))

(defn delete-short-url [url]
  (when-let [params (redis :hget :short-url url)]
    (redis :hdel :short-url params)
    (redis :hdel :short-url url)))

(defn get-short-urls [noteID]
  (redis :smembers (str noteID :urls)))

(defn delete-note [noteID]
  (doseq [kw [:password :views :note :published :edited :publisher]]
    (redis :hdel kw noteID))
  (doseq [url (get-short-urls noteID)]
    (delete-short-url url))
  (redis :del (str noteID :urls)))

(defn create-short-url
  "Creates a short url for the given request metadata or extracts
  one if it was already created"
  [noteID params]
  (let [key (str (into (sorted-map) (clojure.walk/keywordize-keys params)))]
    (if (short-url-exists? key)
      (redis :hget :short-url key)
      (let [hash-stream (partition 5 (repeatedly #(rand-int 36)))
            hash-to-string (fn [hash]
                             (apply str
                                    ; map first 10 numbers to digits
                                    ; and the rest to chars
                                    (map #(char (+ (if (< 9 %) 87 48) %)) hash)))
            url (first
                  (remove short-url-exists?
                          (map hash-to-string hash-stream)))]
        ; we create two mappings: key params -> short url and back,
        ; s.t. we can later easily check whether a short url already exists
        (redis :hset :short-url url key)
        (redis :hset :short-url key url)
        ; we save all short urls of a note for removal later
        (redis :sadd (str noteID :urls) url)
        url))))

(defn gc [password dry]
  (println (get-setting :admin-pw))
  (when (= password (get-setting :admin-pw))
    (let [N 30
          timestamp (- (get-current-date) (* N 24 60 60 1000))
          all-notes (map first (partition 2 (redis :hgetall :note)))
          old-notes (filter #(< (Long/parseLong (redis :hget :published %)) timestamp) all-notes)
          unpopular-notes (filter #(< (try (Long/parseLong (redis :hget :views %))
                                           (catch Exception a 0)) N) old-notes)]
      (println "timestamp:" (str (java.util.Date. timestamp)))
      (doseq [note-id unpopular-notes]
        (do (println (if dry "to be deleted" "deleting") note-id)
            (when-not dry (delete-note note-id))))
      (println (count unpopular-notes) "deleted"))))
