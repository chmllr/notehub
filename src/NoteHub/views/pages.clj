(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use [noir.core :only [defpage]]
        [hiccup.core :only [html]]))

(defpage "/" []
         (common/layout
           [:h1 "Welcome to NoteHub"]))

