(ns NoteHub.storage
  (:refer-clojure :exclude (set get))
  (:require [clj-redis.client :as redis]))

(def db (redis/init))

(defn set [k v]
  (redis/set db k v))

(defn get [k]
  (redis/get db k))
