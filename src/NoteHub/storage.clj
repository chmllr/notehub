(ns NoteHub.storage
  (:require [clj-redis.client :as redis]))

(def db (redis/init))

(def note "note")

(def views "views")

(defn- build-key [[year month day] title]
  (print-str year month day title))

(defn set-note [date title text]
  (let [key (build-key date title)]
    (do
      (redis/hset db views key 0)
      (redis/hset db note key text))))

(defn get-note [date title]
  (let [key (build-key date title)]
    (do
      (redis/hincrby db views key 1)
      (redis/hget db note key))))

(defn get-views [date title]
  (redis/hget db views (build-key date title)))

(defn note-exists? [date title]
  (redis/hexists db note (build-key date title)))

(defn delete-note [date title]
  (redis/hdel db note (build-key date title)))
