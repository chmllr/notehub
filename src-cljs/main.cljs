(ns NoteHub.main
  (:require [goog.crypt.Md5 :as md5]
            [goog.crypt :as crypt]
            [NoteHub.crossover.lib :as lib]))

(defn log
  "Logs to console.log"
  [text]
  (.log js/console text))

(defn $
  "Returns DOM element by Id"
  [id]
  (.getElementById js/document (name id)))

(defn val
  "Returns the value of the element or sets the value if the value was provided"
  ([element] (.-value element))
  ([element value]
   (set! (.-value element) value)))

(defn show
  "show the element"
  [element]
  (set! (.-display (.-style element)) "block"))

; frequently used selectors
(def $draft ($ :draft))
(def $action ($ :action))
(def $preview ($ :preview))
(def $password ($ :password))
(def $plain-password ($ :plain-password))
(def $input-elems ($ :input-elems))
(def $dashed-line ($ :dashed-line))

; Markdown Converter & Sanitizer instantiation
(def md-converter (Markdown.Converter.))

; instantiate & reset a MD5 hash digester
(def md5 (goog.crypt.Md5.))
(.reset md5)

; try to detect iOS
(def ios-detected (.match (.-userAgent js/navigator) "(iPad|iPod|iPhone)"))

(def timer nil)

(def timerDelay
  ; TODO: also test for Android
  (if ios-detected 800 400))

(defn update-preview
  "Updates the preview"
  []
  (do
    (js/clearTimeout timer)
    (let [content (val $draft)
          delay (Math/min timerDelay (* timerDelay (/ (count content) 400)))]
      (def timer
        (js/setTimeout
          #(do
             (show $dashed-line)
             (show $input-elems)
             (set! (.-innerHTML $preview)
                   (.makeHtml md-converter content))) delay)))))

; set focus to the draft textarea (if there is one)
(when $action
  (do
    (if (= "update" (val $action))
      (update-preview)
      (val $draft ""))
    ; foces setting is impossible in iOS, so we border the field instead
    (if ios-detected
      (set! (.-className $draft) (str (.-className $draft) "ui-border"))
      (.focus $draft))))

; show the preview & publish buttons as soon as the user starts typing.
(set! (.-onkeyup $draft) update-preview)

; when the publish button is clicked, compute the hash of the entered text and
; provided session key and assign to the field session-value;
; moreover, compute the password hash as md5 before transmission
(set! (.-onclick ($ :publish-button))
      (fn [e]
        (do
          (.update md5 (val $plain-password))
          (val $plain-password nil)
          (when (val $plain-password)
            (val $password (crypt/byteArrayToHex (.digest md5))))
          (val ($ :session-value) 
               (lib/hash #(.charCodeAt % 0) (str (val $draft) (val ($ :session-key))))))))
