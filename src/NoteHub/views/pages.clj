(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use
    [NoteHub.storage]
    [clojure.string :rename {replace sreplace} :only [split replace lower-case]]
    [clojure.core.incubator :only [-?>]]
    [hiccup.form]
    [noir.response :only [redirect]]
    [noir.core :only [defpage render]]
    [noir.statuses]
    [noir.fetch.remotes])
  (:import 
    [java.util Calendar]
    [org.pegdown PegDownProcessor]))

; Fix a maximal title length used in the link
(def max-title-length 80)

; Markdown -> HTML mapper
(defremote md-to-html [draft]
           (.markdownToHtml (PegDownProcessor.) draft))

; Template for the 404 error
(set-page! 404
           (let [message "Page Not Found."]
             (common/layout message
                            [:article
                             [:h1 message]])))

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
         (common/layout "New Markdown Note"
                        [:div.central-element
                         (form-to [:post "/post-note"]
                                  (text-area {:class "max-width"} :draft)
                                  [:div#buttons.hidden
                                   (submit-button {:style "float: left" :class "button"} "Publish")
                                   [:button#preview-button.button {:type :button :style "float: right"} "Preview"]])]
                        [:div#preview-start-line.hidden]
                        [:article#preview]))

; Note URL
(defpage "/:year/:month/:day/:title" {:keys [year month day title]}
         (let [date [year month day]
               post (get-note date title)
               title (-?> post (split #"\n") first (sreplace #"[_\*#]" ""))]
           (if post
             (common/layout title
                            [:article
                             (md-to-html post)])
             (get-page 400))))

; New Note Posting
(defpage [:post "/post-note"] {:keys [draft]}
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
             (redirect (apply str (interpose "/" ["" year month day title]))))))
