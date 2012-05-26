(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.form]))

(defpage "/" []
         (common/layout "Free Markdown Hosting"
           [:div#hero
            [:h1 "NoteHub"]
            [:h2.helvetica-neue "Free hosting for markdown pages."]
            [:br]
            [:br]
            [:a.button.helvetica-neue {:href "/new"} "New Page"]]))

(defpage "/new" []
         (common/layout "New Markdown Note"
           [:div.central-body.max-width
            (text-area {:class "max-width"} :write-textarea)
            (submit-button {:class "helvetica-neue form-button"} "Publish")]))

