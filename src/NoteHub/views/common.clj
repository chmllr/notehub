(ns NoteHub.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css html5]]))
(use 'cssgen)

(defn gen-comma-list [fonts] (apply str (interpose "," fonts)))

(def global-css (css 
                  (rule ".centerized"
                      :text-align :center)
                  (rule ".helvetica-neue"
                      :font-weight 300
                      :font-family (gen-comma-list ["'Helvetica Neue'"
                                    "Helvetica"
                                    "Arial"
                                    "'Lucida Grande'"
                                    "sans-serif"]))
                  (rule "button"
                        :margin :2em
                        :font-size :1.5em
                        :background :#0a2
                        :color :white
                        :border :none
                        :border-radius :10px
                        :padding :10px)
                  (rule "html, body"
                        :color :#333
                        :margin 0
                        :padding 0)
                  (rule "#hero"
                        :padding-top :5em
                        :padding-bottom :5em
                        :border-bottom [:1px :solid :gray]
                        :text-align :center)
                  (rule "#body"
                        :margin-left "auto"
                        :margin-right "auto"
                        :width "1000px")
                  (rule "h1, h2, h3, h4" :font-family "'Noticia Text'")))

(defpartial layout [& content]
            (html5
              [:head
               [:title "NoteHub"]
               [:link {:href "http://fonts.googleapis.com/css?family=Noticia+Text:400,700" 
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} global-css]]
              [:body
                content]))
