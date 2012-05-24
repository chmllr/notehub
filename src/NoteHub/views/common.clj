(ns NoteHub.views.common
  (:use [noir.core :only [defpartial]]
        [hiccup.page :only [include-css html5]]))
(use 'cssgen)

(defn gen-comma-list [fonts] (apply str (interpose "," fonts)))

(def global-css 
    (css 
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
            :cursor :hand
            :box-shadow [0 :2px :5px :#aaa]
            :margin-top :1em
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
            :text-align :center)
      (rule ".article-font"
            :font-family :Georgia
            :font-size :1.3em)
      (rule ".max-width"
            :width "900px")
      (rule "*:focus"
            :outline [:0px :none :transparent])
      (rule "textarea"
            :font-family :Courier
            :font-size :1.3em
            :border :none
            :height :600px)
      (rule ".central-body"
            :margin-top :5em
            :margin-bottom :5em
            :margin-left "auto"
            :margin-right "auto")
      (rule "h1, h2, h3, h4" :font-family "'Noticia Text'")))

(defpartial layout [& content]
            (html5
              [:head
               [:title "NoteHub"]
               [:link {:href "http://fonts.googleapis.com/css?family=Noticia+Text:400,700" 
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} global-css]]
              [:body content]))
