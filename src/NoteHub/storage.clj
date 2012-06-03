(ns NoteHub.storage
  (:require [clj-redis.client :as redis]))

(def db (redis/init))

(def note "note")

(defn- build-key [[year month day] title]
  (print-str year month day title))

(defn set-note [date title text]
  (redis/hset db note (build-key date title) text))

(defn get-note [date title]
  (redis/hget db note (build-key date title)))

(defn note-exists? [date title]
  (redis/hexists db note (build-key date title)))

(defn delete-note [date title]
  (redis/hdel db note (build-key date title)))
