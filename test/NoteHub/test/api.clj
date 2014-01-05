(ns NoteHub.test.api
  (:require 
        [NoteHub.storage :as storage])
  (:use [NoteHub.api]
        [clojure.test]))

(def note "Hello world, this is a test note!")
(def note2 "Another test note")
(def pid "somePlugin")
(def pid2 "somePlugin2")

(defmacro isnt [arg] `(is (not ~arg)))

(defn register-publisher-fixture [f]
  (def psk (storage/register-publisher pid))
  (f)
  (storage/revoke-publisher pid))

(deftest api
  (testing "API"
    (testing "publisher registration"
      (let [psk2 (storage/register-publisher pid2)]
        (is (storage/valid-publisher? pid))
        (is (storage/valid-publisher? pid2))
        (is (storage/revoke-publisher pid2))
        (isnt (storage/revoke-publisher "anyPID"))
        (isnt (storage/valid-publisher? "any_PID"))
        (isnt (storage/valid-publisher? pid2))))
    #_ (testing "note publishing & retrieval"
      (let [post-response (post-note note pid (get-signature pid psk note))
            get-response (get-note (:noteID post-response))]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        ; TODO: test all response fields!!!!
        (is (= (:longURL post-response) (:longURL get-response)))
        (is (= (:shortURL post-response) (:shortURL get-response))))
      (isnt (:success (:status (post-note note pid (get-signature pid psk note)))))
      (isnt (:success (:status (post-note note pid (get-signature pid "random_psk" note)))))
      (is (:success (:status (post-note note pid (get-signature pid psk note)))))
      (let [psk2 (storage/register-publisher "randomPID")]
        (is (:success (:status (post-note note "randomPID" (get-signature pid psk2 note)))))
        (is (storage/revoke-publisher pid2))
        (isnt (:success (:status (post-note note "randomPID" (get-signature pid psk2 note)))))))
    #_ (testing "note update"
      (let [post-response (post-note note pid (get-signature pid psk note) "passwd")
            note-id (:noteID post-response)
            get-response (get-note note-id)
            new-note "a new note!"
            update-response (update-note note-id new-note pid (get-signature pid psk new-note) "passwd")
            get-response-new (get-note note-id)
            update-response-false (update-note note-id new-note pid (get-signature pid psk new-note) "pass")
            ]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (:success (:status get-response-new)))
        (is (:success (:status update-response)))
        (isnt (:success (:status update-response-false)))
        (is (= note (:note get-response)))
        (is (= new-note (:note get-response-new)))
        (is (= new-note (:note (get-note note-id))))))))
