(ns NoteHub.views.css-generator
  (:use [cssgen]
        [cssgen.types]))

(defn gen-fontlist [& fonts] 
  (apply str 
         (interpose "," 
                    (map #(str "'" % "'") 
                         (filter identity fonts)))))

(def page-width
  (mixin
    :width :800px))
(def helvetica-neue
  (mixin
    :font-weight 300
    :font-family (gen-fontlist "Helvetica Neue"
                                 "Helvetica"
                                 "Arial"
                                 "Lucida Grande"
                                 "sans-serif")))
(def central-element
  (mixin
    page-width
    :margin-top :5em
    :margin-bottom :10em
    :margin-left "auto"
    :margin-right "auto"))

(defn color [theme tone]
  (get-in {:dark {:background :#333
                        :foreground :#ccc
                        :background-halftone :#444
                        :foreground-halftone :#bbb }
           :default {:background :#fff
                          :foreground :#333
                          :background-halftone :#efefef
                          :foreground-halftone :#888 }} [theme tone]))

(defn global-css [params]
  (let [theme (params :theme)
        theme (if theme (keyword theme) :default)
        header-fonts (gen-fontlist (params :header-font) "Noticia Text" "PT Serif" "Georgia")
        text-fonts (gen-fontlist (params :text-font) "Georgia")
        background (color theme :background)
        foreground (color theme :foreground)
        background-halftone (color theme :background-halftone)
        foreground-halftone (color theme :foreground-halftone)]
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
          :background background
          :color foreground
          :margin 0
          :padding 0)
    (rule "#hero"
          :padding-top :5em
          :padding-bottom :5em
          :text-align :center
          (rule "h2"
                helvetica-neue))
    (rule "article"
          central-element
          :line-height (% 140)
          :font-family text-fonts
          :font-size :1.2em
          (rule "& > h1:first-child"
                :text-align :center
                :margin :2em))
    (rule "pre"
          :border-radius :3px
          :padding :1em
          :border [:1px :dotted foreground-halftone]
          :background background-halftone)
    (rule "*:focus"
          :outline [:0px :none :transparent])
    (rule "textarea"
          page-width
          :font-family :Courier
          :font-size :1.2em
          :border :none
          ; TODO: make this dynamic
          :height :500px
          :margin-bottom :2em)
    (rule ".hidden"
          :display :none)
    (rule ".button"
          :border-radius :3px
          helvetica-neue
          :cursor :pointer
          :border [:1px :solid foreground]
          :opacity 0.8
          :font-size :1em
          :background background)
    (rule ".central-element"
          central-element)
    (rule "h1"
          :font-size :2em)
    (rule "#preview-start-line"
          :border-bottom [:1px :dashed foreground-halftone]
          :margin-bottom :5em)
    (rule "h1, h2, h3, h4" 
          :font-family header-fonts))))
