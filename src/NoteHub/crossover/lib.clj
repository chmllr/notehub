(ns NoteHub.crossover.lib
  (:refer-clojure :exclude [hash]))

; very simple hash function %)
; (doesn't work for UTF-16!)
(defn hash [f s]
  (let [short-mod #(mod % 32767)
        ; we cannot use Math/pow because it's imprecise
        ; and differs on JVM and JS
        pow (fn [n e]
              (reduce #(short-mod (* % %2)) 1
                      (repeat e n)))
        char-codes (map #(f (str %)) s)]
    (reduce
      #(short-mod (+ % 
                     (short-mod (* (first %2) (pow 31 (second %2))))))
      0 (map list char-codes (range)))))
