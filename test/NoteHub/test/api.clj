(ns NoteHub.test.api
  (:require 
    [cheshire.core :refer :all]
    [NoteHub.storage :as storage])
  (:use [NoteHub.api]
        [noir.util.test]
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

(deftest api-note-creation
  (testing "Note creation"
    (let [response (send-request [:post "/api/note"]
                                 {:note note
                                  :pid pid
                                  :signature (get-signature pid psk note)
                                  :version "1.0"})
          body (parse-string (:body response))]
      (is (has-status response 200))
      (is (get-in body ["status" "success"]))
      (is (= note ((parse-string
                     (:body (send-request [:get "/api/note"] {:version "1.0" :noteID (body "noteID")}))) "note")))
      (is (do 
            (storage/delete-note (body "noteID"))
            (not (storage/note-exists? (body "noteID"))))))))

#_
(deftest note-update
  (let [session-key (create-session)
        date (get-date)
        title "test-note"
        [year month day] date]
    (testing "Note update"
      (is (has-status 
            (send-request 
              [:post "/post-note"]
              {:session-key session-key
               :draft "test note"
               :password "qwerty"
               :session-value (str (get-hash (str "test note" session-key)))}) 302))
      (is (note-exists? (build-key date title)))
      (is (substring? "test note"
                      ((send-request (url year month day title)) :body)))
      (is (has-status 
            (send-request 
              [:post "/update-note"]
              {:key (build-key [year month day] title)
               :draft "WRONG pass"
               :password "qwerty1" }) 403))
      (is (substring? "test note"
                      ((send-request (url year month day title)) :body)))
      (is (has-status 
            (send-request 
              [:post "/update-note"]
              {:key (build-key [year month day] title)
               :draft "UPDATED CONTENT"
               :password "qwerty" }) 302))
      (is (substring? "UPDATED CONTENT"
                      ((send-request (url year month day title)) :body)))
      (is (do 
            (delete-note (build-key date title))
            (not (note-exists? (build-key date title))))))))
#_

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
