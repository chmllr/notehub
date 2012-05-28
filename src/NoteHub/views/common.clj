(ns NoteHub.views.common
  (:use
    [cssgen]
    [noir.core :only [defpartial]]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

(defn gen-comma-list [& fonts] (apply str (interpose "," fonts)))
(def page-width
  (mixin
    :width :900px))
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
      (rule ".landing-button"
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
                  :background :#0b2))
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
      (rule "article"
            :font-family :Georgia
            :font-size :1.2em
            (rule "& > h1:first-child"
                  :text-align :center
                  :margin :2em))
      (rule "pre"
            :border-radius :3px
            :padding :1em
            :border [:1px :dotted :gray]
            :background :#efefef)
      (rule "*:focus"
            :outline [:0px :none :transparent])
      (rule "textarea"
            page-width
            :font-family :Courier
            :font-size :1.2em
            :border :none
            ; TODO: make this dynamic
            :height :600px
            :margin-bottom :2em)
      (rule ".hidden"
            :display :none)
      (rule ".button"
            :border-radius :3px
            helvetica-neue
            :cursor :pointer
            :border [:1px :solid]
            :opacity 0.8
            :font-size :1em
            :background :white)
      (rule ".central-body"
            page-width
            :margin-top :5em
            :margin-bottom :10em
            :margin-left "auto"
            :margin-right "auto")
      (rule "h1"
            :font-size :2em)
      (rule "#preview-start"
            :border-bottom [:1px :dashed :gray]
            :margin-bottom :5em)
      (rule "h1, h2, h3, h4" 
            :font-family (gen-comma-list
                           "'Noticia Text'" "Georgia"))))

(defpartial layout [title & content]
            (html5
              [:head
               [:title "NoteHub - " title]
               (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
               [:link {:href "http://fonts.googleapis.com/css?family=Noticia+Text:400,700" 
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} global-css]]
              [:body 
                content
                (javascript-tag "var CLOSURE_NO_DEPS = true;")
                (include-js "/cljs/main.js")]))
