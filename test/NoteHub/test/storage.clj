(ns notehub.test.storage
  (:use [notehub.storage]
        [notehub.api :only [build-key]]
        [clojure.test])
  (:require [taoensso.carmine :as car :refer (wcar)]))

(def date [2012 06 03])
(def test-title "Some title.")
(def test-note "This is a test note.")
(def metadata {:year 2012,
               :month 6,
               :day 23,
               :title test-title,
               :theme "dark",
               :header-font "Anton"})
(def test-short-url "")


(deftest storage
  (testing "Storage"
    (testing "of short-url mechanism"
      (let [fakeID (build-key date test-title)
            url (create-short-url fakeID metadata)
            url2 (create-short-url fakeID metadata)]
        (is (= 1 (redis :scard (str fakeID :urls))))
        (def test-short-url (create-short-url fakeID (assoc metadata :a :b)))
        (is (= 2 (redis :scard (str fakeID :urls))))
        (is (short-url-exists? url))
        (is (= url url2))
        (is (= metadata (resolve-url url)))
        (delete-short-url url)
        (is (not (short-url-exists? url))))))
    (testing "of correct note creation"
      (is (= (do
               (add-note (build-key date test-title) test-note "testPID")
               (is (= 2 (redis :scard (str (build-key date test-title) :urls))))
               (create-short-url (build-key date test-title) metadata)
               (is (= 3 (redis :scard (str (build-key date test-title) :urls))))
               (get-note (build-key date test-title)))
             test-note))
      (is (= "1" (get-note-views (build-key date test-title))))
      (is (= (do
               (get-note (build-key date test-title))
               (get-note-views (build-key date test-title)))
             "2")))
    (testing "of note update"
      (is (= (do
               (add-note (build-key date test-title) test-note "testPID" "12345qwert")
               (get-note (build-key date test-title)))
             test-note))
      (is (valid-password? (build-key date test-title) "12345qwert"))
      (is (= (do
               (edit-note (build-key date test-title) "update")
               (get-note (build-key date test-title)))
             "update")))
    (testing "of the note access"
      (is (not= (get-note (build-key date test-title)) "any text")))
    (testing "of note existence"
      (is (note-exists? (build-key date test-title)))
      (is (short-url-exists? test-short-url))
      (is (= 3 (redis :scard (str (build-key date test-title) :urls))))
      (delete-note (build-key date test-title))
      (is (not (short-url-exists? test-short-url)))
      (is (not (note-exists? (build-key date test-title))))
      (is (= 0 (redis :scard (str (build-key date test-title) :urls))))
      (is (not (note-exists? (build-key [2013 06 03] test-title))))
      (is (not (note-exists? (build-key date "some title"))))))
