(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]))

(defpage "/" []
         (common/layout
           [:div#hero
            [:h1 "NoteHub"]
            [:h2.helvetica-neue "Free hosting for markdown pages."]
            [:button.helvetica-neue "Create Page"]]
           [:div#body
            [:p.centerized.helvetica-neue 
              (interpose (repeat 10 "&nbsp")
                         ["About" "How to use" "Impressum"])]]))

