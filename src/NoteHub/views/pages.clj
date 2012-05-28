(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use
    [noir.response :only [content-type]]
    [clojure.string :rename {replace sreplace} :only [trim split replace]]
    [noir.core :only [defpage]]
    [hiccup.form]
    [noir.fetch.remotes])
  (:import [org.pegdown PegDownProcessor]))

(defpage "/" {}
         (common/layout "Free Markdown Hosting"
                        [:div#hero
                         [:h1 "NoteHub"]
                         [:h2 "Free hosting for markdown pages."]
                         [:br]
                         [:br]
                         [:a.landing-button {:href "/new"} "New Page"]]))

(defpage "/new" {}
         (common/layout "New Markdown Note"
                        [:div.central-body
                         (form-to [:get "/preview-note"]
                                  (text-area {:class "max-width"} :draft)
                                  [:div#buttons.hidden
                                   (submit-button {:style "float: left" :class "button"} "Publish")
                                   [:button#preview-button.button {:type :button :style "float: right"} "Preview"]])]
                        [:div#preview-start]
                        [:article#preview.central-body]))

; Actions.

(defremote md-to-html [draft]
           (.markdownToHtml (PegDownProcessor.) draft))
