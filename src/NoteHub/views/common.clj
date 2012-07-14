(ns NoteHub.views.common
  (:use
    [NoteHub.settings :only [get-message]]
    [NoteHub.views.css-generator]
    [noir.core :only [defpartial]]
    [noir.options :only [dev-mode?]]
    [hiccup.util :only [escape-html]]
    [hiccup.core]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

(defn url
  "Creates a local url from the given substrings"
  [& args]
  (apply str (interpose "/" (cons "" args))))

; Creates the main html layout
(defpartial generate-layout 
            [params title & content]
            ; for the sake of security: escape all symbols of the param values
            (let [params (into {} (for [[k v] params] [k (escape-html v)]))]
              (html5
                [:head
                 [:title (print-str (get-message :name) "&mdash;" title)]
                 [:meta {:charset "UTF-8"}]
                 ; generating a link to google's webfonts
                 [:link {:href 
                         (clojure.string/replace
                           (str "http://fonts.googleapis.com/css?family="
                                (apply
                                  str
                                  (interpose "|"
                                             ; ugly thing, but it cannot be avoided since these
                                             ; fonts have to be loaded (independently of CSS)
                                             (concat ["PT+Serif:700" "Noticia+Text:700"]
                                                     (vals (select-keys params 
                                                                        [:header-font :text-font])))))
                                "&subset=latin,cyrillic") " " "+")
                         :rel "stylesheet"
                         :type "text/css"}]
                 ; generating the global CSS
                 [:style {:type "text/css"} (global-css params)]
                 ; google analytics code should appear in prod mode only
                 (if-not (dev-mode?) (include-js "/js/google-analytics.js"))]
                [:body content
                 ; we only need JS during a new note creation, so don't render it otherwise
                 (when (params :js)
                   (html
                     (javascript-tag "var CLOSURE_NO_DEPS = true;")
                     (include-js "https://ajax.googleapis.com/ajax/libs/jquery/1.7.1/jquery.min.js")
                     (include-js "/js/pagedown/Markdown.Converter.js")
                     (include-js "/js/pagedown/Markdown.Sanitizer.js")
                     (include-js "/cljs/main.js")))])))

(defn layout
  "Generates the main html layout"
  [& args]
  ; if some parameter weren't added we provide an empty map
  (if (map? (first args))
    (apply generate-layout args)
    (apply generate-layout {} args)))
