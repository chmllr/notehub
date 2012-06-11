(ns NoteHub.storage
  (:use [NoteHub.settings]
        [noir.options :only [dev-mode?]])
  (:require [clj-redis.client :as redis]))

; Initialize the data base 
(def db 
  (redis/init
    (when (dev-mode?)
      {:url (get-setting :db-url)})))

; DB hierarchy levels
(def note "note")
(def views "views")

; Concatenates all fields to a string
(defn- build-key [[year month day] title]
  (print-str year month day title))

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
