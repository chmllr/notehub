(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]
        [hiccup.form]))

(defpage "/" []
         (common/layout
           [:div#hero
            [:h1 "NoteHub"]
            [:h2.helvetica-neue "Free hosting for markdown pages."]
            [:button.helvetica-neue {:onclick "window.location='/new'"} "New Page"]]))

(defpage "/new" []
         (common/layout
           [:div.central-body.max-width
            (text-area {:class "central-body max-width"} :article-write)]))

