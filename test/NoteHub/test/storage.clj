(ns NoteHub.test.storage
  (:use [NoteHub.storage] [clojure.test]))

(def date [2012 06 03])
(def test-title "Some title.")
(def test-note "This is a test note.")
(def metadata {:year 2012,
               :month 6,
               :day 23,
               :title test-title,
               :theme "dark",
               :header-font "Anton"})


(deftest storage
         (testing "Storage"
                  (testing "of short-url mechanism"
                           (let [url (create-short-url metadata)
                                 url2 (create-short-url metadata)]
                             (is (short-url-exists? url))
                             (is (= url url2))
                             (is (= metadata (resolve-url url)))
                             (is (not (do 
                                        (delete-short-url url)
                                        (short-url-exists? url))))))
                  (testing "of correct note creation"
                           (is (= (do
                                    (set-note date test-title test-note)
                                    (get-note date test-title))
                                  test-note))
                           (is (= "1" (get-note-views date test-title)))
                           (is (= (do
                                    (get-note date test-title)
                                    (get-note-views date test-title))
                                  "2")))
                  (testing "of the note access"
                           (is (not= (get-note date test-title) "any text")))
                  (testing "session management"
                           (let [s1 (create-session)
                                 s2 (create-session)
                                 s3 (create-session)]
                             (is (invalidate-session s1))
                             (is (not (invalidate-session (str s1 s2))))
                             (is (invalidate-session s2))
                             (is (not (invalidate-session "wrongtoken")))
                             (is (invalidate-session s3))))
                  (testing "of note existence"
                           (is (note-exists? date test-title))
                           (is (not (do
                                      (delete-note date test-title) 
                                      (note-exists? date test-title))))
                           (is (not (note-exists? [2013 06 03] test-title)))
                           (is (not (note-exists? date "some title"))))))
