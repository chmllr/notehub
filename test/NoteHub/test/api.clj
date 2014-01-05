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
    (testing "publisher registration"
      (let [psk2 (register-publisher pid2)]
        (is (valid-publisher? pid))
        (is (valid-publisher? pid2))
        (is (revoke-publisher pid2))
        (isnt (revoke-publisher "anyPID"))
        (isnt (valid-publisher? "any_PID"))
        (isnt (valid-publisher? pid2))))
    (testing "note publishing & retrieval"
      (let [post-response (post-note note pid (get-signature pid psk note) ver)
            get-response (get-note ver (:noteID post-response))]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        (is (= (:longURL post-response) (:longURL get-response)))
        (is (= (:shortURL post-response) (:shortURL get-response))))
      (isnt (:success (:status (post-note note pid (get-signature pid psk note) ver))))
      (isnt (:success (:status (post-note note pid (get-signature pid "random_psk" note) ver))))
      (is (:success (:status (post-note note pid (get-signature pid psk note) ver))))
      (let [psk2 (register-publisher "randomPID")]
        (is (:success (:status (post-note note "randomPID" (get-signature pid psk2 note) ver))))
        (is (revoke-publisher pid2))
        (isnt (:success (:status (post-note note "randomPID" (get-signature pid psk2 note) ver))))))
    (testing "note update"
      (let [post-response (post-note note pid (get-signature pid psk note) ver "passwd")
            note-id (:noteID post-response)
            get-response (get-note ver note-id)
            new-note "a new note!"
            update-response (update-note note-id new-note pid (get-signature pid psk new-note) ver "passwd")
            get-response-new (get-note ver note-id)
            update-response-false (update-note note-id new-note pid (get-signature pid psk new-note) ver "pass")
            ]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (:success (:status get-response-new)))
        (is (:success (:status update-response)))
        (isnt (:success (:status update-response-false)))
        (is (= note (:note get-response)))
        (is (= new-note (:note get-response-new)))
        (is (= new-note (:note (get-note note-id))))))))
