(ns NoteHub.storage
  (:require [clj-redis.client :as redis]))

(def db (redis/init))

(def note "note")

(defn- build-key [[year month day] key]
  (print-str year month day key))

(defn set-note [date key v]
  (redis/hset db note (build-key date key) v))

(defn get-note [date key]
  (redis/hget db note (build-key date key)))
