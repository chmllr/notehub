(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:require [NoteHub.crossover.lib :as lib])
  (:use
    [NoteHub.storage]
    [clojure.string :rename {replace sreplace} :only [split replace lower-case]]
    [clojure.core.incubator :only [-?>]]
    [hiccup.form]
    [noir.session :only [flash-put! flash-get]]
    [noir.response :only [redirect status]]
    [noir.core :only [defpage render]]
    [noir.util.crypt :only [encrypt]]
    [noir.statuses]
    [noir.fetch.remotes])
  (:import 
    [java.util Calendar]
    [org.pegdown PegDownProcessor]))

; Fix a maximal title length used in the link
(def max-title-length 80)

; Markdown -> HTML mapper
(defn md-to-html [md-text]
  (.markdownToHtml (PegDownProcessor.) md-text))

(defn get-flash-key []
  (let [k (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (flash-put! k true)
      (print-str k))))

; This function answers to a corresponding AJAX request
(defremote get-preview-md [session-key md]
           (when (flash-get session-key)
             {:session-key (get-flash-key)
              :preview (md-to-html md)}))

; Template for the error sites
(defn page-setter [code message]
  (set-page! code
             (common/layout message
                            [:article
                             [:h1 message]])))

(page-setter 404 "Nothing Found.")
(page-setter 400 "Bad request.")

; Routes
; ======

; Landing Page
(defpage "/" {}
         (common/layout "Free Markdown Hosting"
                        [:div#hero
                         [:h1 "NoteHub"]
                         [:h2 "Free hosting for markdown pages."]
                         [:br]
                         [:a.landing-button {:href "/new"} "New Page"]]))

; New Note Page
(defpage "/new" {}
         (common/layout {:js true} "New Markdown Note"
                        [:div.central-element
                         (form-to [:post "/post-note"]
                                  (hidden-field :session-key (get-flash-key))
                                  (hidden-field {:id :session-value} :session-value)
                                  (text-area {:class :max-width} :draft "Loading...")
                                  [:div#buttons.hidden
                                   (submit-button {:style "float: left"
                                                   :class :button 
                                                   :id :publish-button} "Publish")
                                   [:button#preview-button.button {:type :button 
                                                                   :style "float: right"} "Preview"]])]
                        [:div#preview-start-line.hidden]
                        [:article#preview]))

(defn wrap [params md-text]
    (if md-text 
      (let [title (-?> md-text (split #"\n") first (sreplace #"[_\*#]" ""))]
        (common/layout params title [:article (md-to-html md-text)]))
      (status 404 (get-page 404))))

(defpage "/:year/:month/:day/:title/theme/:theme" {:keys [year month day title theme]}
         (wrap {:theme theme} (get-note [year month day] title)))

(defpage "/:year/:month/:day/:title" {:keys [year month day title]}
         (wrap {:theme :default} (get-note [year month day] title)))

(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
         (let [md-text (get-note [year month day] title)]
           (if md-text md-text (get-page 404))))

; New Note Posting
(defpage [:post "/post-note"] {:keys [draft session-key session-value]}
         (let [valid-session (flash-get session-key) ; it was posted from a newly generated form
               valid-draft (not (empty? draft)) ; the note is non-empty
               valid-hash (try
                            (= (Short/parseShort session-value) ; the hash code is correct 
                               (lib/hash #(.codePointAt % 0) (str draft session-key)))
                            (catch Exception e nil))]
           ; check whether the new note can be added
           (if (and valid-session valid-draft valid-hash)
             (let [[year month day] (map #(+ (second %) (.get (Calendar/getInstance) (first %))) 
                                         {Calendar/YEAR 0, Calendar/MONTH 1, Calendar/DAY_OF_MONTH 0})
                   untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                          (-> draft (split #"\n") first (sreplace " " "-") lower-case))
                   trim (fn [s] (apply str (drop-while #(= \- %) s)))
                   title-uncut (-> untrimmed-line trim reverse trim reverse)
                   proposed-title (apply str (take max-title-length title-uncut))
                   date [year month day] 
                   title (first (drop-while #(note-exists? date %)
                                            (cons proposed-title
                                                  (map #(str proposed-title "-" (+ 2 %)) (range)))))]
               (do
                 (set-note date title draft)
                 ; TODO: the redirect is broken if title contains UTF chars
                 (redirect (apply str (interpose "/" ["" year month day title])))))
             (status 400 ""))))
