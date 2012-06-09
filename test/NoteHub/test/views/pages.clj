(ns NoteHub.test.views.pages
  (:use [NoteHub.views.pages]
        [noir.util.test]
        [NoteHub.storage]
        [clojure.test]))

(def date [2012 6 3])
(def test-title "some-title")
(def test-note "# This is a test note.\nHello _world_.")

(defn create-testnote-fixture [f]
  (set-note date test-title test-note)
  (f)
  (delete-note date test-title))

(use-fixtures :each create-testnote-fixture)

(defn url [& args]
  (apply str (interpose "/" (cons "" args))))

(is (= (url 2010 05 06 "test-title" "export") "/2010/5/6/test-title/export"))

(deftest testing-fixture
         (testing "Was a not created?"
                  (is (= (get-note date test-title) test-note))
                  (is (note-exists? date test-title))))

(deftest helper-functions
         (testing "Markdown generation"
                  (is (= "<h1><em>hello</em> <strong>world</strong></h1><p>test <code>code</code></p>"
                         (md-to-html "#_hello_ __world__\ntest `code`")))))
(deftest export-test
         (testing "Markdown export"
                  (has-body (send-request (url 2012 6 3 "some-title" "export")) test-note)))

(deftest requests
         (testing "HTTP Status"
                  (testing "of a wrong access"
                           (has-status (send-request "/wrong-page") 404)
                           (has-status (send-request (url 2012 6 3 "lol" "stat")) 404)
                           (has-status (send-request (url 2012 6 3 "lol" "export")) 404)
                           (has-status (send-request (url 2012 6 3 "lol")) 404)
                           (has-status (send-request (url 2012 6 4 "wrong-title")) 404))
                  (testing "of corrupt note-post"
                           (has-status (send-request [:post "/post-note"]) 400))
                  (testing "valid accesses"
                           (has-status (send-request "/new") 200)
                           (has-status (send-request (url 2012 6 3 "some-title")) 200)
                           (has-status (send-request (url 2012 6 3 "some-title" "export")) 200)
                           (has-status (send-request (url 2012 6 3 "some-title" "stat")) 200)
                           (has-status (send-request (url 2012 6 3 "some-title")) 200)
                           (has-status (send-request "/") 200))))
