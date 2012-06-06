(ns NoteHub.views.common
  (:use
    [NoteHub.views.css-generator]
    [noir.core :only [defpartial]]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

(defpartial generate-layout 
            [params title & content]
            (html5
              [:head
               [:title "NoteHub &mdash; " title]
               [:link {:href (str "http://fonts.googleapis.com/css?family="
                                  "PT+Serif:700|Noticia+Text:700"
                                  "&subset=latin,cyrillic" )
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} (global-css (params :theme))]]
              (if (params :js)
                [:body content
                 (javascript-tag "var CLOSURE_NO_DEPS = true;")
                 (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
                 (include-js "/cljs/main.js")]
                [:body content])))

(defn layout [& args]
  (if (map? (first args))
    (apply generate-layout args)
    (apply generate-layout {} args)))

