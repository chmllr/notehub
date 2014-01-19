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
(def publisher-key "publisher-key")

(defn valid-publisher? [pid]
  (redis/hexists db publisher-key pid))

(defn register-publisher [pid]
  "Returns nil if given PID exists or a PSK otherwise"
  (when (not (valid-publisher? pid))
    (let [psk (encrypt (str (rand-int Integer/MAX_VALUE) pid))
          _ (redis/hset db publisher-key pid psk)]
      psk)))

(defn revoke-publisher [pid]
  (redis/hdel db publisher-key pid))

(defn get-psk [pid]
  (redis/hget db publisher-key pid))

(defn create-session []
  (let [token (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (redis/sadd db sessions token)
        token)))

(defn invalidate-session [token]
  (let [was-valid (redis/sismember db sessions token)]
    (redis/srem db sessions token)
    was-valid))

(defn edit-note
  [noteID text]
  (do
    (redis/hset db edited noteID (get-current-date))
    (redis/hset db note noteID text)))

(defn add-note
  ([noteID text pid] (add-note noteID text pid nil))
  ([noteID text pid passwd]
   (do
     (redis/hset db note noteID text)
     (redis/hset db published noteID (get-current-date))
     (redis/hset db publisher noteID pid)
     (when (not (blank? passwd))
       (redis/hset db password noteID passwd)))))

(defn valid-password? [noteID passwd]
  (let [stored (redis/hget db password noteID)]
    (and stored (= stored passwd))))

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

(defn get-publisher
  [noteID]
  (redis/hget db publisher noteID))

(defn get-note-statistics
  "Return views, publishing and editing timestamp"
  [noteID]
  {:views (get-note-views noteID)
   :published (redis/hget db published noteID)
   :edited (redis/hget db edited noteID)
   :publisher (get-publisher noteID)})

(defn note-exists?
  [noteID]
  (redis/hexists db note noteID))

(defn delete-note
  [noteID]
  (doseq [kw [password views note published edited publisher]]
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
