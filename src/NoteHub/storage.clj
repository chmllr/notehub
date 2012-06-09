(ns NoteHub.storage
  (:use [NoteHub.config])
  (:require [clj-redis.client :as redis]))

(def db 
  (redis/init
    (when noir.options/dev-mode? 
      {:url (config-map :db-url)})))

(def note "note")

(def views "views")

(defn- build-key [[year month day] title]
  (print-str year month day title))

(defn set-note [date title text]
  (let [key (build-key date title)]
    (redis/hset db note key text)))

(defn get-note [date title]
  (let [key (build-key date title)
        text (redis/hget db note key)]
    (when text
      (do
        (redis/hincrby db views key 1)
        text))))

(defn get-views [date title]
  (redis/hget db views (build-key date title)))

(defn note-exists? [date title]
  (redis/hexists db note (build-key date title)))

(defn delete-note [date title]
  (let [key (build-key date title)]
    (do
      (redis/hdel db views key)
      (redis/hdel db note key))))
