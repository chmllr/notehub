(ns notehub.test.api
  (:require
   [cheshire.core :refer :all]
   [notehub.storage :as storage])
  (:use [notehub.api]
        [notehub.handler]
        [clojure.test]))

(def note "hello world!\nThis is a _test_ note!")
(def pid "somePlugin")
(def pid2 "somePlugin2")
(def note-title (let [[y m d] (get-date)]
                  (apply str (interpose "/" [y m d "hello-world"]))))
(def note-url (str (apply str domain "/" (interpose "/" (get-date))) "/hello-world"))
(defn substring? [a b] (not (= nil (re-matches (re-pattern (str "(?s).*" a ".*")) b))))

(defmacro isnt [arg] `(is (not ~arg)))

(defn send-request
  ([resource] (send-request resource {}))
  ([resource params]
   (let [[method url] (if (vector? resource) resource [:get resource])]
     (app-routes {:request-method method :uri url :params params}))))

(defn has-status [input status]
  (= status (:status input)))

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
      (isnt (:success (:status (get-note {:noteID "some note id"}))))
      (is (= "note is empty" (:message (:status (post-note {:note "" :pid pid :signature (storage/sign pid psk "")})))))
      (let [post-response (post-note {:note note :pid pid :signature (storage/sign pid psk note)})
            get-response (get-note post-response)]
        (is (:success (:status post-response)))
        (is (:success (:status get-response)))
        (is (= note (:note get-response)))
        (is (= (:longURL post-response) (:longURL get-response) note-url))
        (is (= (:shortURL post-response) (:shortURL get-response)))
        (is (storage/note-exists? (:noteID post-response)))
        (let [su (last (clojure.string/split (:shortURL get-response) #"/"))]
          (is (= su (storage/create-short-url (:noteID post-response) (storage/resolve-url su)))))
        (let [resp (send-request
                    (clojure.string/replace (:shortURL get-response) domain ""))
              resp (send-request ((:headers resp) "Location"))]
          (is (substring? "hello world"(:body resp))))
        (is (= (:publisher get-response) pid))
        (is (= (:title get-response) (derive-title note)))
        (is (= "1" (get-in get-response [:statistics :views])))
        (isnt (get-in get-response [:statistics :edited]))
        (is (= "noteID 'randomString' unknown"
               (get-in 
                 (parse-string 
                   (:body (send-request "/api/note" {:version "1.4" :noteID "randomString"})))
                 ["status" "message"])))
        (is (= "3" (get-in (get-note post-response) [:statistics :views])))))
    (testing "creation with wrong signature"
      (let [response (post-note {:note note :pid pid :signature (storage/sign pid2 psk note)})]
        (isnt (:success (:status response)))
        (is (= "signature invalid" (:message (:status response)))))
      (let [response (post-note {:note note :pid pid :signature (storage/sign pid2 psk "any note")})]
        (isnt (:success (:status response)))
        (is (= "signature invalid" (:message (:status response)))))
      (isnt (:success (:status (post-note {:note note :pid pid :signature (storage/sign pid "random_psk" note)}))))
      (is (:success (:status (post-note {:note note :pid pid :signature (storage/sign pid psk note)}))))
      (let [randomPID "randomPID"
            psk2 (storage/register-publisher randomPID)
            _ (storage/revoke-publisher randomPID)
            response (post-note {:note note :pid randomPID :signature (storage/sign randomPID psk2 note)})]
        (isnt (:success (:status response)))
        (is (= (:message (:status response)) "pid invalid"))))
    (testing "note update"
      (let [post-response (post-note {:note note :pid pid :signature (storage/sign pid psk note) :password "passwd"})
            note-id (:noteID post-response)
            new-note "a new note!"]
        (is (:success (:status post-response)))
        (is (:success (:status (get-note {:noteID note-id}))))
        (is (= note (:note (get-note {:noteID note-id}))))
        (let [update-response (update-note {:noteID note-id :note new-note :pid pid
                                            :signature (storage/sign pid psk new-note) :password "passwd"})]
          (isnt (:success (:status update-response)))
          (is (= "signature invalid" (:message (:status update-response)))))
        (is (= note (:note (get-note {:noteID note-id}))))
        (let [update-response (update-note {:noteID note-id :note new-note :pid pid
                                            :signature (storage/sign pid psk note-id new-note "passwd")
                                            :password "passwd"})]
          (is (= { :success true } (:status update-response)))
          (isnt (= nil (get-in (get-note {:noteID note-id}) [:statistics :edited])))
          (is (= new-note (:note (get-note {:noteID note-id})))))
        (let [update-response (update-note {:noteID note-id :note "aaa" :pid pid
                                            :signature (storage/sign pid psk note-id "aaa" "pass")
                                            :password "pass"})]
          (isnt (:success (:status update-response)))
          (is (= "password invalid" (:message (:status update-response)))))
        (is (= new-note (:note (get-note {:noteID note-id}))))
        (is (= new-note (:note (get-note {:noteID note-id}))))))))

(deftest api-note-creation
  (testing "Note creation"
    (let [response (send-request [:post "/api/note"]
                                 {:note note
                                  :pid pid
                                  :signature (storage/sign pid psk note)
                                  :version "1.4"})
          body (parse-string (:body response))
          noteID (body "noteID")]
      (is (has-status response 200))
      (is (get-in body ["status" "success"]))
      (is (= note ((parse-string
                    (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note")))
      (is (= "API version expected" (get-in (parse-string
                                      (:body (send-request [:get "/api/note"] {:noteID noteID}))) ["status" "message"])))
      (is (= note ((parse-string
                    (:body (send-request [:get "/api/note"] {:version "1.1"
                                                             :noteID (clojure.string/replace noteID #"/" " ")}))) "note")))
      (isnt (= note ((parse-string
                    (:body (send-request [:get "/api/note"] {:version "1.4"
                                                             :noteID (clojure.string/replace noteID #"/" " ")}))) "note")))
      (is (do
            (storage/delete-note noteID)
            (not (storage/note-exists? noteID)))))))


(deftest api-note-creation-with-params
  (testing "Note creation with params"
    (let [response (send-request [:post "/api/note"]
                                 {:note note
                                  :pid pid
                                  :signature (storage/sign pid psk note)
                                  :version "1.4"
                                  :theme "dark"
                                  :text-font "Helvetica"})
          body (parse-string (:body response))
          noteID (body "noteID")]
      (let [url ((:headers
                  (send-request
                   (str "/"
                        (last (clojure.string/split
                               ((parse-string (:body response)) "shortURL") #"/"))))) "Location")]
        (= url ((parse-string (:body response)) "longURL"))
        (substring? "theme=dark" url)
        (substring? "text-font=Felvetica" url))
      (is (do
            (storage/delete-note noteID)
            (not (storage/note-exists? noteID)))))))

(deftest api-note-update
  (let [response (send-request [:post "/api/note"]
                               {:note note
                                :pid pid
                                :signature (storage/sign pid psk note)
                                :version "1.4"
                                :password "qwerty"})
        body (parse-string (:body response))
        origID (body "noteID")
        noteID (clojure.string/replace origID #"/" " ")] 
    (testing "Note update"
      (is (has-status response 200))
      (is (get-in body ["status" "success"]))
      (is (storage/note-exists? origID))
      (is (substring? "_test_ note"
                      ((parse-string
                        (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note")))
      (let [response (send-request [:put "/api/note"]
                                   {:noteID noteID
                                    :note "WRONG pass"
                                    :pid pid
                                    :signature (storage/sign pid psk noteID "WRONG pass" "qwerty1")
                                    :password "qwerty1"
                                    :version "1.0"})
            body (parse-string (:body response))]
        (is (has-status response 200))
        (isnt (get-in body ["status" "success"]))
        (is (= "password invalid; this API version is deprecated and will be disabled by the end of June 2014!"
               (get-in body ["status" "message"])))
        (isnt (get-in body ["statistics" "edited"]))
        (is (substring? "_test_ note"
                        ((parse-string
                          (:body (send-request [:get "/api/note"] {:version "1.0" :noteID noteID}))) "note"))))
      (is (get-in (parse-string
                   (:body (send-request [:put "/api/note"]
                                        {:noteID noteID
                                         :note "UPDATED CONTENT"
                                         :pid pid
                                         :signature (storage/sign pid psk noteID "UPDATED CONTENT" "qwerty")
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
