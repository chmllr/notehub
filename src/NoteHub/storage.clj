(ns NoteHub.storage
  (:use [NoteHub.settings]
        [clojure.string :only (blank?)]
        [noir.util.crypt :only [encrypt]]
        [noir.options :only [dev-mode?]])
  (:require [clj-redis.client :as redis]))

; Initialize the data base 
(def db 
  (redis/init
    (when-not (dev-mode?)
      {:url (get-setting :db-url)})))

(defn get-current-date []
  (str (java.util.Date.)))

; DB hierarchy levels
(def note "note")
(def published "published")
(def edited "edited")
(def views "views")
(def password "password")
(def sessions "sessions")
(def short-url "short-url")
(def publisher "publisher")

(defn valid-publisher? [pid]
  (redis/hexists db publisher pid))

(defn register-publisher [pid]
  "Returns nil if given PID exists or a PSK otherwise"
  (when (not (valid-publisher? pid))
    (let [psk (encrypt (str (rand-int Integer/MAX_VALUE) pid))
          _ (redis/hset db publisher pid psk)]
      psk)))

(defn revoke-publisher [pid]
  (redis/hdel db publisher pid))

(defn get-psk [pid]
  (redis/hget db publisher pid))

(defn create-session
  []
  (let [token (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (redis/sadd db sessions token)
        token)))

(defn invalidate-session
  [token]
  ; Jedis is buggy & returns an NPE for token == nil
  (when token
    (let [was-valid (redis/sismember db sessions token)]
      (do (redis/srem db sessions token)
          was-valid))))

(defn update-note
  [noteID text passwd]
  (let [stored-password (redis/hget db password noteID)]
    (when (and stored-password (= passwd stored-password))
      (redis/hset db edited noteID (get-current-date))
      (redis/hset db note noteID text))))

(defn add-note
  ([noteID text] (add-note noteID text nil))
  ([noteID text passwd]
   (do
     (redis/hset db note noteID text)
     (redis/hset db published noteID (get-current-date))
     (when (not (blank? passwd))
       (redis/hset db password noteID passwd)))))

(defn get-note
  [noteID]
  (let [text (redis/hget db note noteID)]
    (when text
      (do
        (redis/hincrby db views noteID 1)
        text))))

(defn get-note-views 
  [noteID]
  (redis/hget db views noteID))

(defn get-note-statistics
  "Return views, publishing and editing timestamp"
  [noteID]
  { :view (redis/hget db views noteID)
    :published (redis/hget db published noteID)
    :edited (redis/hget db edited noteID) })

(defn note-exists?
  [noteID]
  (redis/hexists db note noteID))

(defn delete-note
  [noteID]
  (doseq [kw [password views note]]
    ; TODO: delete short url by looking for the title
    (redis/hdel db kw noteID)))

(defn short-url-exists?
  "Checks whether the provided short url is taken (for testing only)"
  [url]
  (redis/hexists db short-url url))

(defn get-short-url [noteID]
  (redis/hget db short-url noteID))

(defn resolve-url
  "Resolves short url by providing all metadata of the request"
  [url]
  (let [value (redis/hget db short-url url)]
    (when value 
      (read-string value))))

(defn delete-short-url
  "Deletes a short url (for testing only)"
  [noteID]
  (let [value (redis/hget db short-url noteID)]
    (do
      (redis/hdel db short-url noteID)
      (redis/hdel db short-url value))))

(defn create-short-url
  "Creates a short url for the given request metadata or noteID or extracts
  one if it was already created"
  [arg]
  (let [key (if (map? arg) (str (into (sorted-map) arg)) arg)]
    (if (short-url-exists? key)
      (redis/hget db short-url key)
      (let [hash-stream (partition 5 (repeatedly #(rand-int 36)))
            hash-to-string (fn [hash]
                             (apply str 
                                    ; map first 10 numbers to digits
                                    ; and the rest to chars
                                    (map #(char (+ (if (< 9 %) 87 48) %)) hash)))
            url (first 
                  (remove short-url-exists?
                          (map hash-to-string hash-stream)))]
        (do 
          ; we create two mappings: key params -> short url and back,
          ; s.t. we can later easily check whether a short url already exists
          (redis/hset db short-url url key)
          (redis/hset db short-url key url)
          url)))))
