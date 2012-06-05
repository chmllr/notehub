(ns NoteHub.test.views.pages
  (:use [NoteHub.views.pages]
        [noir.util.test]
        [clojure.test]))

(deftest helper-functions
         (testing "Markdown generation"
                  (is (= "<h1><em>hello</em> <strong>world</strong></h1><p>test <code>code</code></p>"
                         (md-to-html "#_hello_ __world__\ntest `code`")))))

(deftest requests
         (testing "HTTP Statuses"
                  (testing "of a wrong access"
                    (has-status (send-request "/wrong-page") 404))
                  (testing "of corrupt note-post"
                    (has-status (send-request [:post "/2012/06/04/wrong-title"]) 404)
                    (has-status (send-request [:post "/post-note"]) 400))
                  (testing "valid accesses"
                    (has-status (send-request "/") 200))))
