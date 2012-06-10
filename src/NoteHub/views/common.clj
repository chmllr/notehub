(ns NoteHub.views.common
  (:use
    [NoteHub.settings :only [get-message]]
    [NoteHub.views.css-generator]
    [noir.core :only [defpartial]]
    [noir.options :only [dev-mode?]]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

; Creates the main html layout
(defpartial generate-layout 
            [params title & content]
            (html5
              [:head
               [:title (print-str (get-message :name) "&mdash;" title)]
               [:link {:href 
                       (clojure.string/replace
                         (str "http://fonts.googleapis.com/css?family="
                              (apply str
                                     (interpose "|" (concat ["PT+Serif:700" "Noticia+Text:700"]
                                                            (vals (select-keys params 
                                                                               [:header-font :text-font])))))
                              "&subset=latin,cyrillic") " " "+")
                       :rel "stylesheet"
                       :type "text/css"}]
               [:style {:type "text/css"} (global-css params)]
               (if-not dev-mode? (include-js "/js/google-analytics.js"))]
              (if (params :js)
                [:body content
                 (javascript-tag "var CLOSURE_NO_DEPS = true;")
                 (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
                 (include-js "/cljs/main.js")]
                [:body content])))

(defn layout
  "Generates the main html layout"
  [& args]
  (if (map? (first args))
    (apply generate-layout args)
    (apply generate-layout {} args)))
