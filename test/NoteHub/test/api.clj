(ns NoteHub.test.api
  (:require 
    [NoteHub.storage :as storage])
  (:use [NoteHub.api]
        [clojure.test]))

(def note "hello world! This is a _test_ note!")
(def pid "somePlugin")
(def pid2 "somePlugin2")
(def note-title (str (apply print-str (get-date)) " hello-world-this-is-a-test-note"))
(def note-url (str domain (apply str (interpose "/" (get-date))) "/hello-world-this-is-a-test-note"))

(defmacro isnt [arg] `(is (not ~arg)))

(defn register-publisher-fixture [f]
  (def psk (storage/register-publisher pid))
  (f)
  (storage/revoke-publisher pid)
  (storage/revoke-publisher pid2)
  (storage/delete-note note-title))

(use-fixtures :each register-publisher-fixture)

(deftest api
  (testing "API"
    (testing "publisher registration"
      (is (storage/valid-publisher? pid))
      (isnt (storage/valid-publisher? pid2))
      (let [psk2 (storage/register-publisher pid2)]
        (is (= psk2 (storage/get-psk pid2)))
        (is (storage/valid-publisher? pid2))
        (is (storage/revoke-publisher pid2))
        (isnt (storage/valid-publisher? "any_PID"))
        (isnt (storage/valid-publisher? pid2))))
    (testing "note publishing & retrieval"
      (isnt (:success (:status (get-note "some note id"))))
      (let [post-response (post-note note pid (get-signature pid psk note))
            get-response (get-note (:noteID post-response))]
        (is (= "note is empty" (:message (:status (post-note "" pid (get-signature pid psk ""))))))
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        (is (= (:longURL post-response) (:longURL get-response) note-url))
        (is (= (:shortURL post-response) (:shortURL get-response)))
        (is (= "1" (get-in get-response [:statistics :views])))
        (isnt (get-in get-response [:statistics :edited]))
        (is (= "2" (get-in (get-note (:noteID post-response)) [:statistics :views])))))
    (testing "creation with wrong signature"
      (let [response (post-note note pid (get-signature pid2 psk note))]
        (isnt (:success (:status response)))
        (is (= "signature invalid" (:message (:status response)))))
      (let [response (post-note note pid (get-signature pid2 psk "any note"))]
        (isnt (:success (:status response)))
        (is (= "signature invalid" (:message (:status response)))))
      (isnt (:success (:status (post-note note pid (get-signature pid "random_psk" note)))))
      (is (:success (:status (post-note note pid (get-signature pid psk note)))))
      (let [randomPID "randomPID"
            psk2 (storage/register-publisher randomPID)
            _ (storage/revoke-publisher randomPID)
            response (post-note note randomPID (get-signature randomPID psk2 note))]
        (isnt (:success (:status response)))
        (is (= (:message (:status response)) "pid invalid"))))
    (testing "note update"
      (let [post-response (post-note note pid (get-signature pid psk note) "passwd")
            note-id (:noteID post-response)
            new-note "a new note!"]
        (is (:success (:status post-response)))
        (is (:success (:status (get-note note-id))))
        (is (= note (:note (get-note note-id))))
        (let [update-response (update-note note-id new-note pid (get-signature pid psk new-note) "passwd")]
          (isnt (:success (:status update-response)))
          (is (= "signature invalid" (:message (:status update-response)))))
        (is (= note (:note (get-note note-id))))
        (let [update-response (update-note note-id new-note pid 
                                           (get-signature pid psk note-id new-note "passwd") "passwd")]
          (is (= { :success true } (:status update-response)))
          (isnt (= nil (get-in (get-note note-id) [:statistics :edited])))
          (is (= new-note (:note (get-note note-id)))))
        (let [update-response (update-note note-id "aaa" pid 
                                           (get-signature pid psk note-id "aaa" "pass") "pass")]
          (isnt (:success (:status update-response)))
          (is (= "password invalid" (:message (:status update-response)))))
        (is (= new-note (:note (get-note note-id))))
        (is (= new-note (:note (get-note note-id))))))))
