(ns NoteHub.crossover.lib
  (:refer-clojure :exclude [hash]))

; very simple hash function %)
; (doesn't work for UTF-16!)
(defn hash [f s]
  (let [short-mod #(mod % 32767)
        char-codes (map f
                        (filter #(not (contains? #{"\n" "\r"} %)) (map str s)))]
    (reduce
      #(short-mod (+ % 
                     (short-mod (* (first %2) 
                                   ((if (odd? %)
                                      bit-xor
                                      bit-and) 16381 (second %2))))))
      0 (map list char-codes (range)))))
