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

; DB hierarchy levels
(def note "note")
(def views "views")
(def password "password")
(def sessions "sessions")
(def short-url "short-url")

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
      (redis/hset db note noteID text))))

(defn add-note
  ([noteID text] (add-note noteID text nil))
  ([noteID text passwd]
   (do
     (redis/hset db note noteID text)
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
  "Returns the number of views for the specified noteID"
  [noteID]
  (redis/hget db views noteID))

(defn note-exists?
  "Returns true if the note with the specified noteID"
  [noteID]
  (redis/hexists db note noteID))

(defn delete-note
  "Deletes the note with the specified coordinates"
  [noteID]
  (doseq [kw [password views note]]
    ; TODO: delete short url by looking for the title
    (redis/hdel db kw noteID)))

(defn short-url-exists?
  "Checks whether the provided short url is taken (for testing only)"
  [url]
  (redis/hexists db short-url url))

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
  "Creates a short url for the given request metadata or extracts
  one if it was already created"
  [metadata]
  (let [request (str (into (sorted-map) metadata))]
    (if (short-url-exists? request)
      (redis/hget db short-url request)
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
          ; we create two mappings: request params -> short url and back,
          ; s.t. we can later easily check whether a short url already exists
          (redis/hset db short-url url request)
          (redis/hset db short-url request url)
          url)))))
