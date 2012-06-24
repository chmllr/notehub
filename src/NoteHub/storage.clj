(ns NoteHub.storage
  (:use [NoteHub.settings]
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
(def short-url "short-url")
(def views "views")
(def sessions "sessions")

; Concatenates all fields to a string
(defn- build-key [[year month day] title]
  (print-str year month day title))

(defn create-session
  "Creates a random session token"
  []
  (let [token (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (redis/sadd db sessions token) token)))

(defn invalidate-session
  "Invalidates given session"
  [token]
  ; Jedis is buggy & returns an NPE for token == nil
  (when token
    (let [was-valid (redis/sismember db sessions token)]
      (do (redis/srem db sessions token) was-valid))))

(defn set-note
  "Creates a note with the given title and text in the given date namespace"
  [date title text]
  (redis/hset db note (build-key date title) text))

(defn get-note
  "Gets the note from the given date namespaces for the specified title"
  [date title]
  (let [key (build-key date title)
        text (redis/hget db note key)]
    (when text
      (do
        (redis/hincrby db views key 1)
        text))))

(defn get-note-views 
  "Returns the number of views for the specified date and note title"
  [date title]
  (redis/hget db views (build-key date title)))

(defn note-exists?
  "Returns true if the note with the specified title and date exists"
  [date title]
  (redis/hexists db note (build-key date title)))

(defn delete-note
  "Deletes the note with the specified coordinates"
  [date title]
  (let [key (build-key date title)]
    (do
      (redis/hdel db views key)
      (redis/hdel db note key))))

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
  [key]
  (let [value (redis/hget db short-url key)]
    (do
      (redis/hdel db short-url key)
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
