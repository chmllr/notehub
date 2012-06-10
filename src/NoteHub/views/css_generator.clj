(ns NoteHub.views.css-generator
  (:use [cssgen]
        [NoteHub.settings]
        [cssgen.types]))

(defn- gen-fontlist [& fonts] 
  (apply str 
         (interpose "," 
                    (map #(str "'" % "'") 
                         (filter identity fonts)))))

; CSS Mixins
(def page-width
  (mixin
    :width (px (get-setting :page-width #(Integer/parseInt %) 800))))

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

; Resolves the theme name & tone parameter to a concrete color
(defn- color [theme tone]
  (get-in {:dark {:background :#333
                  :foreground :#ccc
                  :background-halftone :#444
                  :foreground-halftone :#bbb }
           :default {:background :#fff
                     :foreground :#333
                     :background-halftone :#efefef
                     :foreground-halftone :#888 }} [theme tone]))

(defn global-css 
  "Generates the entire CSS rules of the app"
  [params]
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
      (rule "table,tr,td"
            :margin 0
            :border :none)
      (rule "td"
            :padding :0.5em)
      (rule ".one-third-column"
            :text-align :justify
            :vertical-align :top
            ; Replace this by arithmetic with css-lengths as soon as they fix the bug
            :width (px (quot (get-setting :page-width #(Integer/parseInt %) 800) 3)))
      (rule ".helvetica-neue"
            helvetica-neue)
      (rule "#hero"
            :padding-top :5em
            :padding-bottom :5em
            :text-align :center
            (rule "h1"
                  :font-size :2.5em)
            (rule "h2"
                  helvetica-neue
                  :margin :2em))
      (rule "article"
            central-element
            :line-height (% 140)
            :font-family text-fonts
            :text-align :justify
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
            :font-size :1em
            :border :none
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
