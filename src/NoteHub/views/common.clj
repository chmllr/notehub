(ns NoteHub.views.common
  (:use
    [cssgen]
    [noir.core :only [defpartial]]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

(defn gen-comma-list [& fonts] (apply str (interpose "," fonts)))
(def helvetica-neue
  (mixin
    :font-weight 300
    :font-family (gen-comma-list "'Helvetica Neue'"
                  "Helvetica"
                  "Arial"
                  "'Lucida Grande'"
                  "sans-serif")))

(def global-css 
    (css 
      (rule ".centerized"
          :text-align :center)
      (rule ".button"
            :box-shadow [0 :2px :5px :#aaa]
            :text-decoration :none
            :font-size :1.5em
            :background :#0a2
            :color :white
            :border :none
            :border-radius :10px
            :padding :10px
            helvetica-neue
            (rule "&:hover"
                  :background :#2b3))
      (rule "html, body"
            :color :#333
            :margin 0
            :padding 0)
      (rule "#hero"
            :padding-top :5em
            :padding-bottom :5em
            :text-align :center
            (rule "h2"
                  helvetica-neue))
      (rule ".article-font"
            :font-family :Georgia
            :font-size :1.3em)
      (rule "article"
            :font-family :Georgia
            :font-size :1.2em)
      (rule "*:focus"
            :outline [:0px :none :transparent])
      (rule "textarea"
            :width "900px"
            :font-family :Courier
            :font-size :1.3em
            :border :none
            :height :600px)
      (rule "#preview-button"
            helvetica-neue
            :display :none
            :cursor :pointer
            :border [:1px :solid]
            :background :white
            :font-size :0.8em
            :opacity :0.8)
      (rule ".central-body"
            :width "900px"
            :margin-top :5em
            :margin-bottom :5em
            :margin-left "auto"
            :margin-right "auto")
      (rule "h1"
            :font-size :2em)
      (rule "h1, h2, h3, h4" 
            :font-family (gen-comma-list
                           "'Noticia Text'" "Georgia"))))

(defpartial layout [title & content]
            (html5
              [:head
               [:title "NoteHub - " title]
               [:link {:href "http://fonts.googleapis.com/css?family=Noticia+Text:400,700" 
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} global-css]]
              [:body 
                content
                (javascript-tag "var CLOSURE_NO_DEPS = true;")
                (include-js "/js/main.js")]))
