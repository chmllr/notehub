(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common] [digest :as digest])
  (:use
    [NoteHub.storage :rename {get s-get set s-set} :only [set get]]
    [noir.response :only [content-type]]
    [clojure.string :rename {replace sreplace} :only [split replace lower-case]]
    [noir.core :only [defpage render]]
    [hiccup.form]
    [noir.fetch.remotes])
  (:import [org.pegdown PegDownProcessor]))

(def max-title-length 80)

(defpage "/" {}
         (common/layout "Free Markdown Hosting"
                        [:div#hero
                         [:h1 "NoteHub"]
                         [:h2 "Free hosting for markdown pages."]
                         [:br]
                         [:a.landing-button {:href "/new"} "New Page"]]))

(defpage "/new" {}
         (common/layout "New Markdown Note"
                        [:div.central-body
                         (form-to [:post "/post-note"]
                                  (text-area {:class "max-width"} :draft)
                                  [:div#buttons.hidden
                                   (submit-button {:style "float: left" :class "button"} "Publish")
                                   [:button#preview-button.button {:type :button :style "float: right"} "Preview"]])]
                        [:div#preview-start]
                        [:article#preview.central-body]))

(defpage "/:year/:month/:day/:title" {:keys [year month day title]}
         (let [key (str "note-" (digest/md5 (str year month day title)))]
           (common/layout "TEST"
             [:article.central-body
              (md-to-html (s-get key))])))

(defpage [:post "/post-note"] {:keys [draft]}
         (let [[year month day] (split (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.)) #"-")
               untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                      (-> draft (split #"\n") first (sreplace " " "-") lower-case))
               trim (fn [s] (apply str (drop-while #(= \- %) s)))
               title-uncut (-> untrimmed-line trim reverse trim reverse)
               title (apply str (take max-title-length title-uncut))
               key (str "note-" (digest/md5 (str year month day title)))]
           (do
             (s-set key draft)
             (render "/:year/:month/:day/:title" {:year year :month month :day day :title title}))))

; Actions.

(defremote md-to-html [draft]
           (.markdownToHtml (PegDownProcessor.) draft))
