(ns NoteHub.crossover.lib
  (:refer-clojure :exclude [hash]))

(defn hash 
  "A simple hash-function, which computes a hash from the text field 
  content and given session number. It is intended to be used as a spam
  protection / captcha alternative. (Probably doesn't work for URF-16)"
  [f s]
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
