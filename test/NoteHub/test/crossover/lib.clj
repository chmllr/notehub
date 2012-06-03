(ns NoteHub.test.crossover.lib
  (:require [NoteHub.crossover.lib :as lib])
  (:use [clojure.test]))

(with-test
  (defn lib-hash [s]
    (lib/hash #(.codePointAt % 0) s))
  (testing "Crossover Lib:"
           (testing "Self-made hash function"
                    (testing "for correct hashes"
                             (is (= 0 (lib-hash "")))
                             (is (= 6178 (lib-hash "test тест")))
                             (is (= 6178 (lib-hash (str "test\n \rтест"))))
                             (is (= 274 (lib-hash "Hello world!"))))
                    (testing "for a wrong hash"
                             (is (not= 6178 (lib-hash "wrong hash")))))))
