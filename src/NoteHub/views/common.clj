(ns NoteHub.views.common
  (:use
    [NoteHub.settings :only [get-message]]
    [noir.core :only [defpartial]]
    [noir.options :only [dev-mode?]]
    [hiccup.util :only [escape-html]]
    [ring.util.codec :only [url-encode]]
    [hiccup.core]
    [hiccup.page :only [include-js html5]]
    [hiccup.element :only [javascript-tag]]))

(defn url
  "Creates a local url from the given substrings"
  [& args]
  (apply str (interpose "/" (cons "" (map url-encode args)))))

; Creates the main html layout
(defpartial generate-layout 
  [params title & content]
  ; for the sake of security: escape all symbols of the param values
  (let [params (into {} (for [[k v] params] [k (escape-html v)]))
        theme (:theme params "default")]
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
       [:link {:rel "stylesheet/less" :type "text/css" :href "/style.less"}]
       ; google analytics code should appear in prod mode only
       (if-not (dev-mode?) (include-js "/js/google-analytics.js"))]
      [:body content
       ; we only need JS during a new note creation, so don't render it otherwise
       (html
         (include-js "/js/less.js")
         (include-js "/js/md5.js")
         (include-js "/js/marked.js")
         (include-js "/js/main.js")
         (include-js "/js/themes.js")
         (javascript-tag (str "less.modifyVars({
                          '@background': themes['" theme "'].background.normal,
                          '@background_halftone': themes['" theme "'].background.halftone,
                          '@foreground': themes['" theme "'].foreground.normal,
                          '@foreground_halftone': themes['" theme "'].foreground.halftone,
                          '@link_fresh': themes['" theme "'].link.fresh,
                          '@link_visited': themes['" theme "'].link.visited,
                          '@link_hover': themes['" theme "'].link.hover});")))])))

(defn layout
  "Generates the main html layout"
  [& args]
  ; if some parameter weren't added we provide an empty map
  (if (map? (first args))
    (apply generate-layout args)
    (apply generate-layout {} args)))
