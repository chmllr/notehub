(ns NoteHub.views.pages
  (:require [NoteHub.crossover.lib :as lib]
            [clojure.contrib.string :as ccs])
  (:use
    [NoteHub.storage]
    [NoteHub.settings]
    [NoteHub.views.common]
    [clojure.string :rename {replace sreplace} :only [split replace lower-case]]
    [clojure.core.incubator :only [-?>]]
    [hiccup.form]
    [ring.util.codec :only [url-encode]]
    [hiccup.core]
    [hiccup.util :only [escape-html]]
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

; Creates a random session token
(defn- get-flash-key []
  (let [k (encrypt (str (rand-int Integer/MAX_VALUE)))]
    (do (flash-put! k true)
      (print-str k))))

; Sets a custom message for each needed HTTP status.
; The message to be assigned is extracted with a dynamically generated key
(doseq [code [400 404 500]]
  (set-page! code
             (let [message (get-message (keyword (str "status-" code)))]
               (layout message
                       [:article [:h1 message]]))))

; shortcut for rendering an HTTP status
(defn- response [code]
  (status code (get-page code)))

; Converts given markdown to html and wraps with the main layout
(defn- wrap [params md-text]
  (if md-text 
    (let [title (-?> md-text (split #"\n") first (sreplace #"[_\*#]" ""))]
      (layout params title [:article (md-to-html md-text)]))
    (status 404 (get-page 404))))

; Routes
; ======

; This function answers to an AJAX request: it gets a session key and a markdown text.
; It returns the html code of the provided markdown and a new session key.
(defremote get-preview-md [session-key md]
           (when (flash-get session-key)
             {:session-key (get-flash-key)
              :preview (md-to-html md)}))

; Landing Page
(defpage "/" {}
         (layout (get-message :title)
                 [:div#hero
                  [:h1 (get-message :name)]
                  [:h2 (get-message :title)]
                  [:br]
                  [:a.landing-button {:href "/new" :style "color: white"} (get-message :new-page)]]
                 [:div.dashed-line]
                 [:table.central-element.helvetica-neue
                  [:tr
                   ; dynamically generates three column, retrieving corresponding messages
                   (for [e [:column-why :column-how :column-geeks]]
                     (html  
                       [:td.one-third-column
                        [:h2 (get-message e)] (md-to-html (get-message (keyword (str (name e) "-long"))))]))]]
                 [:div.centered.helvetica-neue (md-to-html (get-message :created-by))]))

; New Note Page
(defpage "/new" {}
         (layout {:js true} (get-message :new-note)
                 [:div.central-element
                  (form-to [:post "/post-note"]
                           (hidden-field :session-key (get-flash-key))
                           (hidden-field {:id :session-value} :session-value)
                           (text-area {:class :max-width} :draft (get-message :loading))
                           [:div#buttons.hidden
                            (submit-button {:style "float: left"
                                            :class :button 
                                            :id :publish-button} (get-message :publish))
                            [:button#preview-button.button {:type :button 
                                                            :style "float: right"} (get-message :preview)]])]
                 [:div#preview-start-line.dashed-line.hidden]
                 [:article#preview]))

; Displays the note
(defpage "/:year/:month/:day/:title" {:keys [year month day title theme header-font text-font] :as params}
         (wrap 
           (select-keys params [:theme :header-font :text-font])
           (get-note [year month day] title)))

; Provides Markdown of the specified note
(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
         (let [md-text (get-note [year month day] title)]
           (if md-text md-text (response 404))))

; Provides the number of views of the specified note
(defpage "/:year/:month/:day/:title/stat" {:keys [year month day title]}
         (let [views (get-note-views [year month day] title)]
           (if views 
             (layout (get-message :statistics)
                      [:table.helvetica-neue.central-element
                       [:tr
                        [:td (get-message :published)]
                        [:td (interpose "-" [year month day])]]
                       [:tr
                        [:td (get-message :article-views)]
                        [:td views]]])
             (response 404))))

; New Note Posting — the most "complex" function in the entire app ;)
(defpage [:post "/post-note"] {:keys [draft session-key session-value]}
         ; first we collect all info needed to evaluate the validity of the note creation request
         (let [valid-session (flash-get session-key) ; was the note posted from a newly generated form?
               valid-draft (not (ccs/blank? draft)) ; has the note a meaningful content?
               ; is the hash code correct?
               valid-hash (try
                            (= (Short/parseShort session-value) 
                               (lib/hash #(.codePointAt % 0) (str draft session-key)))
                            (catch Exception e nil))]
           ; check whether the new note can be added
           (if (and valid-session valid-draft valid-hash)
             ; if yes, we compute the current date, extract a title string from the text,
             ; which will be a part of the url and look whether this title is free today;
             ; if not, append "-n", where "n" is the next free number
             (let [[year month day] (map #(+ (second %) (.get (Calendar/getInstance) (first %))) 
                                         {Calendar/YEAR 0, Calendar/MONTH 1, Calendar/DAY_OF_MONTH 0})
                   untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                          (-> draft ccs/split-lines first (sreplace " " "-") lower-case))
                   trim (fn [s] (apply str (drop-while #(= \- %) s)))
                   title-uncut (-> untrimmed-line trim reverse trim reverse)
                   max-length (get-setting :max-title-length #(Integer/parseInt %) 80)
                   ; TODO: replace to ccs/take when it gets fixed
                   proposed-title (apply str (take max-length title-uncut))
                   date [year month day] 
                   title (first (drop-while #(note-exists? date %)
                                            (cons proposed-title
                                                  (map #(str proposed-title "-" (+ 2 %)) (range)))))]
               (do
                 (set-note date title draft)
                 (redirect (url year month day (url-encode title)))))
             (response 400))))
