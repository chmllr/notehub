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
