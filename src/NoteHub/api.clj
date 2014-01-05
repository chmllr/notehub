(ns NoteHub.api
  (:require [NoteHub.storage :as persistance]))

(def api-version "1.0")

(defn- create-response
  ([success] { :success success })
  ([success message]
   (assoc (create-response success) :message message)))

(let [md5Instance (java.security.MessageDigest/getInstance "MD5")]
  (defn get-signature
    "Returns the MD5 hash for the concatenation of all passed parameters"
    [& args]
    (let [input (apply str args)]
      (do (.reset md5Instance)
          (.update md5Instance (.getBytes input))
          (.toString (new java.math.BigInteger 1 (.digest md5Instance)) 16)))))

(defn get-note [noteID])
(defn post-note [& args])
(defn update-note [& args])
(defn register-publisher [& args])
(defn revoke-publisher [& args])
(defn valid-publisher? [& args])
