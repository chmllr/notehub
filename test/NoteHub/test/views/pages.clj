(ns NoteHub.test.views.pages
  (:require [NoteHub.crossover.lib :as lib])
  (:use [NoteHub.views.pages]
        [noir.util.test]
        [clojure.contrib.string :only [substring?]]
        [NoteHub.views.common :only [url]]
        [NoteHub.storage]
        [clojure.test]))

(def date [2012 6 3])
(def test-title "some-title")
(def test-note "# This is a test note.\nHello _world_. Motörhead, тест.")

(defn create-testnote-fixture [f]
  (set-note date test-title test-note)
  (f)
  (delete-note date test-title))

(use-fixtures :each create-testnote-fixture)

(is (= (url 2010 05 06 "test-title" "export") "/2010/5/6/test-title/export"))

(deftest testing-fixture
         (testing "Was a not created?"
                  (is (= (get-note date test-title) test-note))
                  (is (note-exists? date test-title))))

(deftest helper-functions
         (testing "Markdown generation"
                  (is (= "<h1><em>hellö</em> <strong>world</strong></h1><p>test <code>code</code></p>"
                         (md-to-html "#_hellö_ __world__\ntest `code`")))))
(deftest export-test
         (testing "Markdown export"
                  (is (has-body (send-request (url 2012 6 3 "some-title" "export")) test-note))))

(deftest note-creation
         (let [session-key (create-session)
               date (get-date)
               ; TODO: replace note generation by a function from pages.clj
               title "this-is-a-test-note"
               [year month day] date]
           (testing "Note creation"
                    (is (has-status 
                          (send-request 
                            [:post "/post-note"]
                            {:session-key session-key
                             :draft test-note
                             :session-value (str (lib/hash #(.codePointAt % 0) 
                                                           (str test-note session-key)))}) 302))
                    (is (note-exists? date title))
                    (is (substring? "Hello <em>world</em>"
                                    ((send-request (url year month day title)) :body)))
                    (is (do 
                          (delete-note date title)
                          (not (note-exists? date title)))))))

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
