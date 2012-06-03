(ns NoteHub.test.all
  (:use [NoteHub.storage] [clojure.test]))

(def date [2012 06 03])
(def test-title "Some title.")
(def test-note "This is a test note.")

(testing "Storage"
         (testing "of correct note creation"
                  (is (= (do
                           (set-note date test-title test-note)
                           (get-note date test-title))
                         test-note)))
         (testing "of the note access"
                  (is (not= (get-note date test-title) "any text")))
         (testing "of note existence"
                  (is (note-exists? date test-title))
                  (is (not (do
                             (delete-note date test-title) 
                             (note-exists? date test-title))))
                  (is (not (note-exists? [2013 06 03] test-title)))
                  (is (not (note-exists? date "some title")))))
