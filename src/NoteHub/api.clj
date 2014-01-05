(ns NoteHub.api
  (:import 
    [java.util Calendar])
  (:use
    [NoteHub.settings]
    [clojure.string :rename {replace sreplace}
     :only [replace blank? lower-case split-lines]])
  (:require [NoteHub.storage :as storage]))

(def api-version "1.0")

(def domain "http://notehub.org/")

; Concatenates all fields to a string
(defn build-key 
  "Returns a storage-key for the given note coordinates"
  [[year month day] title]
  (print-str year month day title))

(defn get-date
  "Returns today's date"
  []
  (map #(+ (second %) (.get (Calendar/getInstance) (first %))) 
       {Calendar/YEAR 0, Calendar/MONTH 1, Calendar/DAY_OF_MONTH 0}))

(defn- create-response
  ([success] { :success success })
  ([success message & params]
   (assoc (create-response success) :message (apply format message params))))

(defn- getURL [noteID description]
  (if (description)
    (str domain (storage/get-short-url noteID))
    (str domain (sreplace noteID #" " "/"))))

(let [md5Instance (java.security.MessageDigest/getInstance "MD5")]
  (defn get-signature
    "Returns the MD5 hash for the concatenation of all passed parameters"
    [& args]
    (let [input (apply str args)]
      (do (.reset md5Instance)
          (.update md5Instance (.getBytes input))
          (.toString (new java.math.BigInteger 1 (.digest md5Instance)) 16)))))

(defn get-note [noteID]
  {:note (storage/get-note noteID)
   :longURL (getURL noteID)
   :shortURL (getURL noteID :short)
   :statistics (storage/get-note-statistics noteID)
   :status (if (storage/note-exists? noteID)
             (create-response true)
             (create-response false "noteID '%s' unknown" noteID))})

(defn post-note
  ([note pid signature] (post-note note pid signature nil))
  ([note pid signature password]
  (let [errors (filter identity
                       (lazy-seq
                         [(when-not (storage/valid-publisher? pid) "pid invalid")
                          (when-not (= signature
                                       (get-signature pid (storage/get-psk pid) note))
                            "signature invalid")
                          (when (blank? note) "note is empty")]))]
    (if (empty? errors)
      (let [[year month day] (get-date)
            untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                   (-> note split-lines first (sreplace " " "-") lower-case))
            trim (fn [s] (apply str (drop-while #(= \- %) s)))
            title-uncut (-> untrimmed-line trim reverse trim reverse)
            max-length (get-setting :max-title-length #(Integer/parseInt %) 80)
            proposed-title (apply str (take max-length title-uncut))
            date [year month day] 
            title (first (drop-while #(storage/note-exists? (build-key date %))
                                     (cons proposed-title
                                           (map #(str proposed-title "-" (+ 2 %)) (range)))))
            noteID (build-key date title)]
        (do
          (storage/add-note noteID note password)
          (storage/create-short-url noteID)
          {
           :noteID noteID
           :longURL (getURL noteID)
           :shortURL (getURL noteID :short)
           :status (create-response true)
           }))
      {:status (create-response false (first errors))}))))


(defn update-note [noteID note pid signature password]
  (let [errors (filter identity
                       (lazy-seq
                         [(when-not (storage/valid-publisher? pid) "pid invalid")
                          (when-not (= signature
                                       (get-signature pid (storage/get-psk pid) noteID note password))
                            "signature invalid")
                          (when (blank? note) "note is empty")
                          (when-not (storage/update-note noteID note password) "password invalid")]))]
    (if (empty? errors)
      {
       :longURL (getURL noteID)
       :shortURL (getURL noteID :short)
       :status (create-response true)
       }
      {:status (create-response false (first errors))})))
