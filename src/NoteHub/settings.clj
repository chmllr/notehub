(ns NoteHub.settings
  (:refer-clojure :exclude [replace reverse])
  (:use [clojure.string]))

; Load and parse te settings file returning a map
(def settings-map
  (let [file-content (slurp "settings")
        lines (split file-content #"\n")
        pairs (map #(map trim (split % #"=")) lines)]
    (apply hash-map 
           (mapcat #(list (keyword (first %)) (second %)) pairs))))

(defn get-setting
  "Takes a settings key, a converter function and a default value, and returns a corresponding 
  setting value. The default value is returned back when no setting value was found.
  The converter function can be provided to convert the setting from string to a needed type."
  [key & more]
  (let [converter (first more)
        default (second more)
        value (settings-map key)
        ; Through this hack we can read security-critical settings from (previously 
        ; set) shell variables without commiting their content to CVS
        value (if-not value 
                (System/getenv 
                  (upper-case
                    (replace (name key) #"-" ""))))]
    (if value
      (if (fn? converter) (converter value) value)
      default)))
