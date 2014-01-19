(ns NoteHub.test.api
  (:require
   [cheshire.core :refer :all]
   [NoteHub.storage :as storage])
  (:use [NoteHub.api]
        [noir.util.test]
        [clojure.test]))

(def note "hello world!\nThis is a _test_ note!")
(def pid "somePlugin")
(def pid2 "somePlugin2")
(def note-title (str (apply print-str (get-date)) " hello-world"))
(def note-url (str (apply str domain "/" (interpose "/" (get-date))) "/hello-world"))
(defn substring? [a b] (not (= nil (re-matches (re-pattern (str "(?s).*" a ".*")) b))))

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
      (is (= "note is empty" (:message (:status (post-note "" pid (get-signature pid psk ""))))))
      (let [post-response (post-note note pid (get-signature pid psk note))
            get-response (get-note (:noteID post-response))]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        (is (= (:longURL post-response) (:longURL get-response) note-url))
        (is (= (:shortURL post-response) (:shortURL get-response)))
        (is (storage/note-exists? (:noteID post-response)))
        (let [su (last (clojure.string/split (:shortURL get-response) #"/"))]
          (is (= su (storage/create-short-url (storage/resolve-url su)))))
        (let [resp (send-request
                                (clojure.string/replace (:shortURL get-response) domain ""))
              resp (send-request ((:headers resp) "Location"))]
        (is (substring? "hello world"(:body resp))))
        (is (= (:publisher get-response) pid))
        (is (= (:title get-response) (derive-title note)))
        (is (= "1" (get-in get-response [:statistics :views])))
        (isnt (get-in get-response [:statistics :edited]))
        (is (= "3" (get-in (get-note (:noteID post-response)) [:statistics :views])))))
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

(deftest api-note-creation
  (testing "Note creation"
    (let [response (send-request [:post "/api/note"]
                                 {:note note
                                  :pid pid
                                  :signature (get-signature pid psk note)
                                  :version "1.0"})
          body (parse-string (:body response))
          noteID (body "noteID")]
      (is (has-status response 200))
      (is (get-in body ["status" "success"]))
      (is (= note ((parse-string
                    (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note")))
      (is (do
            (storage/delete-note noteID)
            (not (storage/note-exists? noteID)))))))

(deftest note-update
  (let [response (send-request [:post "/api/note"]
                               {:note note
                                :pid pid
                                :signature (get-signature pid psk note)
                                :version "1.0"
                                :password "qwerty"})
        body (parse-string (:body response))
        noteID (body "noteID")]
    (testing "Note update"
      (is (has-status response 200))
      (is (get-in body ["status" "success"]))
      (is (storage/note-exists? noteID))
      (is (substring? "_test_ note"
                      ((parse-string
                        (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note")))
      (let [response (send-request [:put "/api/note"]
                                   {:noteID noteID
                                    :note "WRONG pass"
                                    :pid pid
                                    :signature (get-signature pid psk noteID "WRONG pass" "qwerty1")
                                    :password "qwerty1"
                                    :version "1.0"})
            body (parse-string (:body response))]
        (is (has-status response 200))
        (isnt (get-in body ["status" "success"]))
        (is (= "password invalid" (get-in body ["status" "message"])))
        (isnt (get-in body ["statistics" "edited"]))
        (is (substring? "_test_ note"
                        ((parse-string
                          (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note"))))
      (is (get-in (parse-string
                   (:body (send-request [:put "/api/note"]
                                        {:noteID noteID
                                         :note "UPDATED CONTENT"
                                         :pid pid
                                         :signature (get-signature pid psk noteID "UPDATED CONTENT" "qwerty")
                                         :password "qwerty"
                                         :version "1.0"}))) ["status" "success"]))
      (isnt (= nil (((parse-string
                      (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID})))
                     "statistics") "edited")))
      (is (substring? "UPDATED CONTENT"
                      ((parse-string
                        (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note")))
      (is (do
            (storage/delete-note noteID)
            (not (storage/note-exists? noteID)))))))
