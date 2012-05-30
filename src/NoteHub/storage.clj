(ns NoteHub.storage
  (:require [clj-redis.client :as redis]))

(def db (redis/init))

(def note "note")

(def draft "draft")

(defn- build-key [[year month day] key]
  (print-str year month day key))

(defn set-note [date key v]
  (redis/hset db note (build-key date key) v))

(defn get-note [date key]
  (redis/hget db note (build-key date key)))

(defn create-draft [key]
  (redis/hget db draft key))

(defn delete-draft [key]
  (redis/hdel db draft key))

(defn draft-exists? [key]
  (redis/hexists db draft key))

(defn note-exists? [date key]
  (redis/hexists db note (build-key date key)))
