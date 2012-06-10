(ns NoteHub.crossover.lib
  (:refer-clojure :exclude [hash]))

(defn hash 
  "A simple hash-function, which computes a hash from the text field 
  content and given session number. It is intended to be used as a spam
  protection / captcha alternative (let's see whether spambots evaluate heavy JS). 
  (Probably doesn't work for UTF-16)"
  [f s]
  (let [short-mod #(mod % 32767)
        char-codes (map f (remove #(contains? #{"\n" "\r"} %) (map str s)))
        zip-with-index (map list char-codes (range))]
    (reduce
      #(short-mod (+ % 
                     (short-mod (* (first %2) 
                                   ((if (odd? %)
                                      bit-xor
                                      bit-and) 16381 (second %2))))))
      0 zip-with-index)))
