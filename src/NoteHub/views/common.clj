(ns NoteHub.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page-helpers :only [include-css html5]]))
(use 'cssgen)

(defpartial layout [& content]
            (html5
              [:head
               [:title "NoteHub"]
               [:style {:type "text/css"}
                (css (rule "h1" :font-family "Impact"))]
               ]
              [:body
               ; TODO: replace wrapper with smth
               [:div#wrapper
                content]]))
