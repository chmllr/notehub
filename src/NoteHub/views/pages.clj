(ns NoteHub.views.pages
  (:require [hiccup.util :as util]
            [NoteHub.api :as api]
            [cheshire.core :refer :all])
  (:use
    [NoteHub.storage] ; TODO: delete this
    [NoteHub.settings]
    [NoteHub.views.common]
    [clojure.string :rename {replace sreplace}
     :only [escape split replace blank? split-lines lower-case]]
    [clojure.core.incubator :only [-?>]]
    [hiccup.form]
    [hiccup.core]
    [hiccup.element]
    [noir.response :only [redirect status content-type]]
    [noir.core :only [defpage defpartial]]
    [noir.statuses]))

(defn get-hash 
  "A simple hash-function, which computes a hash from the text field 
  content and given session number. It is intended to be used as a spam
  protection / captcha alternative. (Probably doesn't work for UTF-16)"
  [s]
  (let [short-mod #(mod % 32767)
        char-codes (map #(.codePointAt % 0) (remove #(contains? #{"\n" "\r"} %) (map str s)))
        zip-with-index (map list char-codes (range))]
    (reduce
      #(short-mod (+ % 
                     (short-mod (* (first %2) 
                                   ((if (odd? %)
                                      bit-xor
                                      bit-and) 16381 (second %2))))))
      0 zip-with-index)))

; Sets a custom message for each needed HTTP status.
; The message to be assigned is extracted with a dynamically generated key
(doseq [code [400 403 404 500]]
  (set-page! code
             (let [message (get-message (keyword (str "status-" code)))]
               (layout message
                       [:article [:h1 message]]))))

; shortcut for rendering an HTTP status
(defn- response [code]
  (status code (get-page code)))

; Converts given markdown to html and wraps with the main layout
(defn- wrap [short-url params md-text]
  (when md-text 
    (layout params (params :title)
            [:article.bottom-space.markdown md-text]
            (let [links (map #(link-to 
                                (if (= :short-url %)
                                  (url short-url)
                                  (str (params :title) "/" (name %)))
                                (get-message %))
                             [:stats :edit :export :short-url])
                  space (apply str (repeat 4 "&nbsp;"))
                  separator (str space "&middot;" space)
                  links (interpose separator links)]
              [:div#panel (map identity links)]))))

; Routes
; ======

; Landing Page
(defpage "/" {}
  (layout (get-message :page-title)
          [:div#hero
           [:h1 (get-message :name)]
           [:h2 (get-message :title)]
           [:br]
           [:a.landing-button {:href "/new" :style "color: white"} (get-message :new-page)]]
          [:div#dashed-line]
          [:article.helvetica.bottom-space.markdown {:style "font-size: 1em"} 
           (slurp "LANDING.md")]
          [:div.centered.helvetica.markdown (get-message :footer)]))

; input form for the markdown text with a preview area
(defpartial input-form [form-url command fields content passwd-msg]
  (let [css-class (when (= :publish command) :hidden)]
    (layout {:js true} (get-message :new-note)
            [:article#preview.markdown " "]
            [:div#dashed-line {:class css-class}]
            [:div.central-element.helvetica {:style "margin-bottom: 3em"}
             (form-to {:autocomplete :off} [:post form-url]
                      (hidden-field :action command)
                      (hidden-field :password)
                      fields
                      (text-area {:class :max-width} :draft content)
                      [:fieldset#input-elems {:class css-class}
                       (text-field {:class "ui-elem" :placeholder (get-message passwd-msg)} 
                                   :plain-password)
                       (submit-button {:class "button ui-elem"
                                       :id :publish-button} (get-message command))])])))

; Update Note Page
(defpage "/:year/:month/:day/:title/edit" {:keys [year month day title]}
  (let [noteID (api/build-key [year month day] title)]
    (input-form "/update-note" :update 
                (html (hidden-field :key noteID))
                (get-note noteID) :enter-passwd)))

; New Note Page
(defpage "/new" {}
  (input-form "/post-note" :publish
              (html (hidden-field :session-key (create-session))
                    (hidden-field {:id :session-value} :session-value))
              (get-message :loading) :set-passwd))

; Displays the note
(defpage "/:year/:month/:day/:title" {:keys [year month day title theme header-font text-font] :as params}
  (wrap 
    (create-short-url params)
    (select-keys params [:title :theme :header-font :text-font])
    (get-note (api/build-key [year month day] title))))

; Provides Markdown of the specified note
(defpage "/:year/:month/:day/:title/export" {:keys [year month day title]}
  (when-let [md-text (get-note (api/build-key [year month day] title))]
    (content-type "text/plain; charset=utf-8" md-text)))

; Provides the number of views of the specified note
(defpage "/:year/:month/:day/:title/stats" {:keys [year month day title]}
  (when-let [views (get-note-views (api/build-key [year month day] title))]
    (layout (get-message :statistics)
            [:table#stats.helvetica.central-element
             [:tr
              [:td (get-message :published)]
              [:td (interpose "-" [year month day])]]
             [:tr
              [:td (get-message :article-views)]
              [:td views]]])))

; Updates a note
(defpage [:post "/update-note"] {:keys [key draft password]}
  (if (update-note key draft password)
    (redirect (apply url (split key #" ")))
    (response 403)))

; New Note Posting — the most "complex" function in the entire app ;)
(defpage [:post "/post-note"] {:keys [draft password session-key session-value]}
  ; first we collect all info needed to evaluate the validity of the note creation request
  (let [valid-session (invalidate-session session-key) ; was the note posted from a newly generated form?
        valid-draft (not (blank? draft)) ; has the note a meaningful content?
        ; is the hash code correct?
        valid-hash (try
                     (= (Short/parseShort session-value) 
                        (get-hash (str draft session-key)))
                     (catch Exception e nil))]
    ; check whether the new note can be added
    (if (and valid-session valid-draft valid-hash)
      ; if yes, we compute the current date, extract a title string from the text,
      ; which will be a part of the url and look whether this title is free today;
      ; if not, append "-n", where "n" is the next free number
      (let [[year month day] (api/get-date)
            untrimmed-line (filter #(or (= \- %) (Character/isLetterOrDigit %)) 
                                   (-> draft split-lines first (sreplace " " "-") lower-case))
            trim (fn [s] (apply str (drop-while #(= \- %) s)))
            title-uncut (-> untrimmed-line trim reverse trim reverse)
            max-length (get-setting :max-title-length #(Integer/parseInt %) 80)
            ; TODO: replace to ccs/take when it gets fixed
            proposed-title (apply str (take max-length title-uncut))
            date [year month day] 
            title (first (drop-while #(note-exists? (api/build-key date %))
                                     (cons proposed-title
                                           (map #(str proposed-title "-" (+ 2 %)) (range)))))]
        (do
          (add-note (api/build-key date title) draft password)
          (redirect (url year month day title))))
      (response 400))))

; Resolving of a short url
(defpage "/:short-url" {:keys [short-url]}
  (when-let [params (resolve-url short-url)]
    (let [{:keys [year month day title]} params
          rest-params (dissoc params :year :month :day :title)
          core-url (url year month day title)
          long-url (if (empty? rest-params) core-url (util/url core-url rest-params))]
      (redirect long-url))))

; Here lives the API

(defpage "/api" args
  (let [title (get-message :api-registration)]
  (layout title [:article.markdown (slurp "API.md")])))

(defpage [:get "/api/note"] {:keys [version noteID]}
  (generate-string (api/get-note noteID)))

(defpage [:post "/api/note"] {:keys [version note pid signature password]}
  (generate-string (api/post-note note pid signature password)))

(defpage [:put "/api/note"] {:keys [version noteID note pid signature password]}
  (generate-string (api/update-note noteID note pid signature password)))





