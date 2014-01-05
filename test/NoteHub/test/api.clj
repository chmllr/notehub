(ns NoteHub.test.api
  (:use [NoteHub.api] [clojure.test]))

(def note "Hello world, this is a test note!")
(def note2 "Another test note")
(def pid "somePlugin")
(def pid2 "somePlugin2")
(def ver api-version)

(defmacro isnt [arg] `(is (not ~arg)))

(defn register-publisher-fixture [f]
  (def psk (register-publisher pid))
  (f)
  (revoke-publisher pid))

(deftest api
  (testing "API"
    (testing "signature implementation"
      (is (= 3577853521 (get-signature "Lorem ipsum dolor sit amet" "abcdef")))
      (is (= -180217198 (get-signature "Notehub is a free pastebin for markdown" "12345678")))
      (is (= 6887137804 (get-signature "abcd !§$%& параграф" "A VERY LONG KEY"))))
    (testing "publisher registration"
      (let [psk2 (register-publisher pid2)]
        (is (valid-publisher? pid))
        (is (valid-publisher? pid2))
        (is (revoke-publisher pid2))
        (isnt (revoke-publisher "anyPID"))
        (isnt (valid-publisher? "any_PID"))
        (isnt (valid-publisher? pid2))))
    (testing "note publishing & retrieval"
      (let [post-response (post-note note pid (get-signature note psk) ver)
            get-response (get-note ver (:noteID post-response))]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        (is (= (:longURL post-response) (:longURL get-response)))
        (is (= (:shortURL post-response) (:shortURL get-response))))
      (isnt (:success (:status (post-note note pid (get-signature note2 psk) ver))))
      (isnt (:success (:status (post-note note pid (get-signature note "random_psk") ver))))
      (is (:success (:status (post-note note pid (get-signature note psk) ver))))
      (let [psk2 (register-publisher "randomPID")]
        (is (:success (:status (post-note note "randomPID" (get-signature note psk2) ver))))
        (is (revoke-publisher pid2))
        (isnt (:success (:status (post-note note "randomPID" (get-signature note psk2) ver))))))
    (testing "note update"
      (let [post-response (post-note note pid (get-signature note psk) ver "passwd")
            note-id (:noteID post-response)
            get-response (get-note ver note-id)
            new-note "a new note!"
            update-response (update-note note-id new-note pid (get-signature new-note psk) ver "passwd")
            get-response-new (get-note ver note-id)
            update-response-false (update-note note-id new-note pid (get-signature new-note psk) ver "pass")
            ]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (:success (:status get-response-new)))
        (is (:success (:status update-response)))
        (isnt (:success (:status update-response-false)))
        (is (= note (:note get-response)))
        (is (= new-note (:note get-response-new)))
        (is (= new-note (:note (get-note note-id))))))))
