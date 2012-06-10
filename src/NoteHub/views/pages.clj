(ns NoteHub.views.pages
  (:require [NoteHub.crossover.lib :as lib])
  (:use
    [NoteHub.storage]
    [NoteHub.settings]
    [NoteHub.views.common]
    [clojure.string :rename {replace sreplace} :only [split replace lower-case]]
    [clojure.core.incubator :only [-?>]]
    [hiccup.form]
    [noir.session :only [flash-put! flash-get]]
    [noir.response :only [redirect status]]
    [noir.core :only [defpage render]]
    [noir.util.crypt :only [encrypt]]
    [noir.statuses]
    [noir.fetch.remotes])
  (:import 
    [java.util Calendar]
    [org.pegdown PegDownProcessor]))

; Markdown -> HTML mapper
(defn md-to-html [md-text]
  (.markdownToHtml (PegDownProcessor.) md-text))

; Creates a random session number
(defn- get-flash-key []
  (let [k (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (flash-put! k true)
      (print-str k))))

; Converts given markdwon to html and wraps with layout
(defn- wrap [params md-text]
  (if md-text 
    (let [title (-?> md-text (split #"\n") first (sreplace #"[_\*#]" ""))]
      (layout params title [:article (md-to-html md-text)]))
    (status 404 (get-page 404))))

; Template for the error sites
(defn page-setter [code message]
  (set-page! code
             (layout message
                     [:article
                      [:h1 message]])))

; Sets a message for each corresponding HTTP status
(page-setter 404 "Nothing Found.")
(page-setter 400 "Bad request.")
(page-setter 500 "OMG, Server Exploded.")

; Routes
; ======

; This function answers to a AJAX request: it gets a sesion key and markdown text.
; IT return html version of the provided markdown and a new session key
(defremote get-preview-md [session-key md]
           (when (flash-get session-key)
             {:session-key (get-flash-key)
              :preview (md-to-html md)}))

; Landing Page
(defpage "/" {}
         (layout "Free Markdown Hosting"
                 [:div#hero
                  [:h1 "NoteHub"]
                  [:h2 "Free and hassle-free hosting for markdown pages."]
                  [:br]
                  [:a.landing-button {:href "/new"} "New Page"]]
                  [:div#preview-start-line]
                  [:table.central-element.helvetica-neue
                   [:tr
                    [:td.one-third-column
                     [:h2 "Why?"]
                     "Not every person, who occasionally wants to express some thoughts, needs a blog.
                     Blogs are <b>tedious</b> for writers and for readers. Most people are not interested in thoughts
                     of other random people. Moreover, nowadays everything rotates around social networks and not
                     individual blogs. It makes much more sense to publish something somewhere and to share
                     the link with the audience on the community or social network of your choice, than to maintain a blog
                     trying to keep your readers interested.
                     <b>NoteHub</b> should be the place, where you can publish your thoughts without hassle."]
                    [:td.one-third-column
                     [:h2 "How to Use?"]
                     "First create a new page using the markdown syntax. Now, besides just sharing the link, you can
                     view some rudimentary statistics by appending <code>/stats</code> to the note url:
                     <pre>notehub.org/.../title/stats</pre>
                     If you want to export a note in the original Markdown format, append <code>/export</code>
                     <pre>notehub.org/.../title/export</pre>
                     And if you want, you also can invert the color scheme by appending <code>?theme=dark</code> to the note url.
                     <pre>notehub.org/.../title?theme=dark</pre>"]
                    [:td.one-third-column
                     [:h2 "For Geeks!"]
                     "NoteHub was an experiment and is implemented entirely in Clojure and ClojureScript. Its source code can 
                     be found on GitHub. Look at the code to find some undocumented NoteHub features (or bugs) and &mdash; feel free to contribute!
                     (If you are interested in more detailed code overview, read the following note.) NoteHub's design
                     is intentionally kept extremelly simple and minimalistic, and should stay like this.
                     NoteHub's persistence layer bases on the key-value store redis.
                     Currently, NoteHub is hosted for free on Heroku.
                     Send your feedback and comments directly to @chmllr."]]]))

; New Note Page
(defpage "/new" {}
         (layout {:js true} "New Markdown Note"
                 [:div.central-element
                  (form-to [:post "/post-note"]
                           (hidden-field :session-key (get-flash-key))
                           (hidden-field {:id :session-value} :session-value)
                           (text-area {:class :max-width} :draft "Loading...")
                           [:div#buttons.hidden
                            (submit-button {:style "float: left"
                                            :class :button 
                                            :id :publish-button} "Publish")
                            [:button#preview-button.button {:type :button 
                                                            :style "float: right"} "Preview"]])]
                 [:div#preview-start-line.hidden]
                 [:article#preview]))

; Display the note
(defpage "/:year/:month/:day/:title" {:keys [year month day title theme header-font text-font] :as params}
         (wrap 
           (select-keys params [:theme :header-font :text-font])
           (get-note [year month day] title)))

; Provides Markdown of the specified note
(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
         (let [md-text (get-note [year month day] title)]
           (if md-text md-text (status 404 (get-page 404)))))

; Provides the number of views of the specified note
(defpage "/:year/:month/:day/:title/stat" {:keys [year month day title]}
         (let [views (get-views [year month day] title)]
           (if views 
             (layout "Statistics"
                     [:article.helvetica-neue
                      [:table {:style "width: 100%"}
                       [:tr
                        [:td "Published"]
                        [:td (interpose "-" [year month day])]]
                       [:tr
                        [:td "Article views"]
                        [:td views]]]])
             (status 404 (get-page 404)))))

; New Note Posting
(defpage [:post "/post-note"] {:keys [draft session-key session-value]}
         (let [valid-session (flash-get session-key) ; it was posted from a newly generated form
               valid-draft (not (empty? draft)) ; the note is non-empty
               valid-hash (try
                            (= (Short/parseShort session-value) ; the hash code is correct 
                               (lib/hash #(.codePointAt % 0) (str draft session-key)))
                            (catch Exception e nil))]
           ; check whether the new note can be added
           (if (and valid-session valid-draft valid-hash)
             (let [[year month day] (map #(+ (second %) (.get (Calendar/getInstance) (first %))) 
                                         {Calendar/YEAR 0, Calendar/MONTH 1, Calendar/DAY_OF_MONTH 0})
                   untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                          (-> draft (split #"\n") first (sreplace " " "-") lower-case))
                   trim (fn [s] (apply str (drop-while #(= \- %) s)))
                   title-uncut (-> untrimmed-line trim reverse trim reverse)
                   proposed-title (apply str (take (get-setting :max-title-length #(Integer/parseInt %) 80) 
                                                   title-uncut))
                   date [year month day] 
                   title (first (drop-while #(note-exists? date %)
                                            (cons proposed-title
                                                  (map #(str proposed-title "-" (+ 2 %)) (range)))))]
               (do
                 (set-note date title draft)
                 ; TODO: the redirect is broken if title contains UTF chars
                 (redirect (apply str (interpose "/" ["" year month day title])))))
             (status 400 ""))))
