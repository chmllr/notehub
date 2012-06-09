(ns NoteHub.settings
  (:require [clojure.string :as cs]))

(defn get-setting
  "Takes a settings key, a default value and a converter function and returns a corresponding 
  settings value.  The default value is returned back when no setting value was found.
  The converter function can be provided to convert the setting from string to a needed format."
  [key & more]
  (let [default (first more)
        converter (second more)
        file-content (slurp "settings")
        lines (cs/split file-content #"\n")
        pairs (map #(map cs/trim %) (map #(cs/split % #"=") lines))
        config-map (apply hash-map (mapcat #(list (keyword (first %)) (second %)) pairs))
        value (config-map key)
        ; Through this hack we can read security-critical settings from (previously 
        ; set) shell variables without commiting their content to CVS
        value (if-not value 
                (System/getenv 
                  (cs/upper-case
                    (cs/replace (name key) #"-" ""))))]
    (if value
      (if (fn? converter) (converter value) value)
      default)))
