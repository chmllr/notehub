(ns NoteHub.test.storage
  (:use [NoteHub.storage]
        [NoteHub.api :only [build-key]]
        [NoteHub.views.pages]
        [clojure.test]))

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
               (add-note (build-key date test-title) test-note "testPID")
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
      (is (note-exists? (build-key date test-title)))
      (is (not (do
                 (delete-note (build-key date test-title))
                 (note-exists? (build-key date test-title)))))
      (is (not (note-exists? (build-key [2013 06 03] test-title))))
      (is (not (note-exists? (build-key date "some title")))))))
