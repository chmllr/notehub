(ns notehub.css
  (:require [garden.core :refer [css]]
            [garden.stylesheet :refer [at-media]]
            [garden.units :as u :refer [px pt em]]))

(def themes 
  {
   "dark" 
   {
    :background {
                 :normal "#333",
                 :halftone "#444"
                 },
    :foreground {
                 :normal "#ccc",
                 :halftone "#bbb"
                 },
    :link {
           :fresh "#6b8",
           :visited "#496",
           :hover "#7c9"
           }
    },
   "solarized-light" 
   {
    :background {
                 :normal "#fdf6e3",
                 :halftone "#eee8d5"
                 },
    :foreground {
                 :normal "#657b83",
                 :halftone "#839496"
                 },
    :link {
           :fresh "#b58900",
           :visited "#cb4b16",
           :hover "#dc322f"
           }
    },
   "solarized-dark" 
   {
    :background {
                 :normal "#073642",
                 :halftone "#002b36"
                 },
    :foreground {
                 :normal "#93a1a1",
                 :halftone "#839191"
                 },
    :link {
           :fresh "#cb4b16",
           :visited "#b58900",
           :hover "#dc322f"
           }
    },
   "default" 
   {
    :background {
                 :normal "#fff",
                 :halftone "#efefef"
                 },
    :foreground {
                 :normal "#333",
                 :halftone "#888"
                 },
    :link {
           :fresh "#097",
           :visited "#054",
           :hover "#0a8"
           }
    }
   }
  )

(defn generate [params]
  (let [theme (themes (params "theme" "default"))
        ; VARIABLES
        background (get-in theme [:background :normal])
        foreground (get-in theme [:foreground :normal])
        background-halftone (get-in theme [:background :halftone])
        foreground-halftone (get-in theme [:foreground :halftone])
        link-fresh (get-in theme [:link :fresh])
        link-visited (get-in theme [:link :visited])
        link-hover (get-in theme [:link :hover])
        width (px 800)
        header-font (or (params "header-font") "Noticia Text")
        text-font (or (params "text-font") "Georgia")
        header-size-factor (Float/parseFloat (or (params "header-size") "1"))
        text-size-factor (Float/parseFloat (or (params "text-size") "1"))

        ; MIXINS
        helvetica {
                   :font-weight 300
                   :font-family "'Helvetica Neue','Helvetica','Arial','Lucida Grande','sans-serif'"
                   }
        central-element {
                         :margin-left "auto"
                         :margin-right "auto"
                         }
        thin-border {
                     :border (print-str "1px solid" foreground)
                     }]
    (css
      [:.ui-border { :border-radius (px 3) } thin-border]

      [:a {
           :color link-fresh
           :text-decoration "none"
           :border-bottom "1px dotted"
           }]
      [:a:hover { :color link-hover }]
      [:a:visited { :color link-visited }]
      [:#draft {
                :margin-bottom (em 3)
                }]
      [:.button {
                 :cursor "pointer"
                 }]
      [:.ui-elem {
                  :border-radius (px 3)
                  :padding (em 0.3)
                  :opacity 0.8
                  :font-size (em 1)
                  :background background
                  }
       helvetica thin-border]
      [:.landing-button, :textarea, :fieldset { :border "none" }]
      [:.landing-button {
                         :box-shadow "0 2px 5px #aaa"
                         :text-decoration "none"
                         :font-size (em 1.5)
                         :background "#0a2"
                         :border-radius (px 10)
                         :padding (px 10)
                         }
       helvetica]
      [:.landing-button:hover { :background "#0b2" }]

      [:.helvetica helvetica]

      [:#footer {
                 :width "100%"
                 :font-size (em 0.8)
                 :padding-bottom (em 1)
                 :text-align "center"
                 }
       helvetica]
      (at-media {:screen true :max-width (px 767)} [:#footer {:font-size (em 0.4)}])
      ["#footer a" { :border "none" }]

      [:html, :body {
                     :background background
                     :color foreground
                     :margin 0
                     :padding 0
                     }]
      [:#hero {
               :padding-top (em 5)
               :padding-bottom (em 5)
               :text-align "center"
               }]
      [:h1, :h2, :h3, :h4, :h5, :h6 {
                                     :font-weight "bold"
                                     :font-family (str header-font ",'Noticia Text','PT Serif','Georgia'")
                                     }]
      [:h1 { :font-size (em (* 1.8 header-size-factor)) }]
      [:h2 { :font-size (em (* 1.6 header-size-factor)) }]
      [:h3 { :font-size (em (* 1.4 header-size-factor)) }]
      [:h4 { :font-size (em (* 1.2 header-size-factor)) }]
      [:h5 { :font-size (em (* 1.1 header-size-factor)) }]
      [:h6 { :font-size (em (* 1 header-size-factor)) }]

      ["#hero h1" { :font-size (em 2.5) }]
      ["#hero h2" { :margin (em 2) } helvetica ]

      [:article {
                 :font-family (str text-font ", 'Georgia'")
                 :margin-top (em 5)
                 :text-align "justify"
                 :flex 1
                 :-webkit-flex 1
                 }
       central-element]

      (at-media {:screen true :min-width (px 1024)} [:article {:width width}])
      (at-media {:screen true :max-width (px 1023)} [:article {:width "90%"}])

      [:.central-element central-element]

      (at-media {:screen true :min-width (px 1024)} [:.central-element {:width width}])
      (at-media {:screen true :max-width (px 1023)} [:.central-element {:width "90%"}])

      ["article p" {
                    :font-size (em (* 1.2 text-size-factor))
                    :line-height "140%"
                    }]
      ["article > h1:first-child" {
                                   :text-align "center"
                                   :font-size (em (* 2 header-size-factor))
                                   :margin (em 2)
                                   }]

      [:.centered { :text-align "center" }]
      [:.bottom-space { :margin-bottom (em 7) }]
      [:code, :pre {
                    :font-family "monospace"
                    :background background-halftone
                    :font-size (em (* 1.2 text-size-factor))
                    }]
      
      [:pre {
             :border-radius (px 3)
             :padding (em 0.5)
             :border (str "1px dotted" foreground-halftone)
             }]
      
      ["*:focus" { :outline "0px none transparent" }]
      (at-media {:screen true :min-width (px 1024)} [:textarea {:width width}])
      
      [:textarea {
                  :border-radius (px 5)
                  :font-family "Courier"
                  :font-size (em 1)
                  :height (px 500)
                  }]
      [:.hidden { :display "none" }]
      [:#dashed-line {
                      :border-bottom (str "1px dashed" foreground-halftone)
                      :margin-top (em 3)
                      :margin-bottom (em 3)
                      }]
      [:table {
               :width "100%"
               :border-collapse "collapse"
               }]
      [:th {
            :padding (em 0.3)
            :background-color background-halftone
            }]
      [:td {
            :border-top (str "1px dotted" foreground-halftone)
            :padding (em 0.3)
            :line-height (em 2.5)
            }]
      [:.middot { :padding (em 0.5) }]
      
      [:body { :display "-webkit-flex" }]
      
      [:body {
              :display "flex"
              :min-height "100vh"
              :flex-direction "column"
              :-webkit-flex-direction "column"
              }]
  )))
