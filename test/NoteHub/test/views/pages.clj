(ns NoteHub.test.views.pages
  (:use [NoteHub.views.pages]
        [NoteHub.api :only [build-key get-signature get-date]]
        [noir.util.test]
        [NoteHub.views.common :only [url]]
        [NoteHub.storage]
        [clojure.test]))

(defn substring? [a b] (not (= nil (re-matches (re-pattern (str "(?s).*" a ".*")) b))))
(def date [2012 6 3])
(def test-title "some-title")
(def test-note "# This is a test note.\nHello _world_. Motörhead, тест.")

(defn create-testnote-fixture [f]
  (add-note (build-key date test-title) test-note)
  (f)
  (delete-note (build-key date test-title)))

(use-fixtures :each create-testnote-fixture)

(is (= (url 2010 05 06 "test-title" "export") "/2010/5/6/test-title/export"))

(deftest testing-fixture
  (testing "Was a not created?"
    (is (= (get-note (build-key date test-title)) test-note))
    (is (note-exists? (build-key date test-title)))))

(deftest export-test
  (testing "Markdown export"
    (is (has-body (send-request (url 2012 6 3 "some-title" "export")) test-note))))

(deftest note-creation
  (let [session-key (create-session)
        date (get-date)
        title "this-is-a-test-note"
        [year month day] date]
    (testing "Note creation"
      (is (has-status 
            (send-request 
              [:post "/post-note"]
              {:session session-key
               :note test-note
               :signature (get-signature session-key test-note)}) 302))
      (is (note-exists? (build-key date title)))
      (is (substring? "Hello _world_"
                      ((send-request (url year month day title)) :body)))
      (is (do 
            (delete-note (build-key date title))
            (not (note-exists? (build-key date title))))))))

(deftest note-creation-utf
  (let [session-key (create-session)
        date (get-date)
        title "радуга"
        note "# Радуга\nкаждый охотник желает знать, где сидят фазаны."
        [year month day] date]
    (testing "Note creation with UTF8 symbols"
      (is (has-status 
            (send-request 
              [:post "/post-note"]
              {:session session-key
               :note note
               :signature (get-signature session-key note)}) 302))
      (is (note-exists? (build-key date title)))
      (is (substring? "знать" ((send-request (url year month day title)) :body)))
      (is (do 
            (delete-note (build-key date title))
            (not (note-exists? (build-key date title))))))))

(deftest note-update
  (let [session-key (create-session)
        date (get-date)
        title "this-is-a-test-note"
        [year month day] date
        hash (get-signature session-key test-note)]
    (testing "Note update"
      (is (has-status 
            (send-request 
              [:post "/post-note"]
              {:session session-key
               :note test-note
               :password "qwerty"
               :signature hash}) 302))
      (is (note-exists? (build-key date title)))
      (is (substring? "test note" ((send-request (url year month day title)) :body)))
      (is (has-status 
            (send-request 
              [:post "/update-note"]
              {:noteID (build-key [year month day] title)
               :note "WRONG pass"
               :password "qwerty1" }) 403))
      (is (substring? "test note" ((send-request (url year month day title)) :body)))
      (is (has-status 
            (send-request 
              [:post "/update-note"]
              {:noteID (build-key [year month day] title)
               :note "UPDATED CONTENT 123"
               :password "qwerty" }) 302))
      (is (substring? "UPDATED CONTENT" ((send-request (url year month day title)) :body)))
      (is (do 
            (delete-note (build-key date title))
            (not (note-exists? (build-key date title))))))))

(deftest requests
  (testing "HTTP Status"
    (testing "of a wrong access"
      (is (has-status (send-request "/wrong-page") 404))
      (is (has-status (send-request (url 2012 6 3 "lol" "stat")) 404))
      (is (has-status (send-request (url 2012 6 3 "lol" "export")) 404))
      (is (has-status (send-request (url 2012 6 3 "lol")) 404))
      (is (has-status (send-request (url 2012 6 4 "wrong-title")) 404)))
    (testing "of corrupt note-post"
      (is (has-status (send-request [:post "/post-note"]) 400)))
    (testing "valid accesses"
      ;(is (has-status (send-request "/new") 200) "accessing /new")
      (is (has-status (send-request (url 2012 6 3 "some-title")) 200) "accessing test note")
      (is (has-status (send-request (url 2012 6 3 "some-title" "export")) 200) "accessing test note's export")
      (is (has-status (send-request (url 2012 6 3 "some-title" "stats")) 200) "accessing test note's stats")
      (is (has-status (send-request "/") 200) "accessing landing page"))))
