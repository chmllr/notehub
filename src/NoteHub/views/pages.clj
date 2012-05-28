(ns NoteHub.views.pages
  (:require [NoteHub.views.common :as common])
  (:use
        [clojure.string :rename {replace sreplace} :only [trim split replace]]
        [noir.core :only [defpage]]
        [hiccup.form])
  (:import [org.pegdown PegDownProcessor]))

(defpage "/" {}
         (common/layout "Free Markdown Hosting"
           [:div#hero
            [:h1 "NoteHub"]
            [:h2 "Free hosting for markdown pages."]
            [:br]
            [:br]
            [:a.button {:href "/new"} "New Page"]]))

(defpage "/new" {}
         (common/layout "New Markdown Note"
           [:div.central-body
            (form-to [:post "/preview-note"]
              (text-area {:class "max-width"} :draft)
              (submit-button {:id "preview-button"} "Preview"))]))

; Actions.

(defpage [:post "/preview-note"] {:keys [draft]}
         (let [get-title (comp trim #(sreplace % "#" "") first #(split % #"\n"))]
           (common/layout (get-title draft)
              [:article.central-body
                (.markdownToHtml (PegDownProcessor.) draft)])))
