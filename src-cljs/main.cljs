(ns NoteHub.main
  (:use [jayq.core :only [$ xhr css inner val anim show]])
  (:require [goog.dom :as gdom]
            [goog.crypt.Md5 :as md5]
            [goog.crypt :as crypt]
            [NoteHub.crossover.lib :as lib]
            [clojure.browser.dom :as dom]))

; frequently used selectors
(def $draft ($ :#draft))
(def $action ($ :#action))
(def $preview ($ :#preview))
(def $password ($ :#password))
(def $plain-password ($ :#plain-password))
(def $input-elems ($ :#input-elems))
(def $dashed-line ($ :#dashed-line))

; Markdown Converter & Sanitizer instantiation
(def md-converter (Markdown/getSanitizingConverter))

; instantiate & reset a MD5 hash digester
(def md5 (goog.crypt.Md5.))
(.reset md5)

; try to detect iOS
(def ios-detected (.match (.-userAgent js/navigator) "(iPad|iPod|iPhone)"))

(defn update-preview
  []
  "Updates the preview"
  (do
    (show $dashed-line)
    (show $input-elems)
    (inner $preview
           (.makeHtml md-converter (val $draft)))))

; set focus to the draft textarea (if there is one)
(when $action
  (do
    (if (= "update" (val $action))
      (update-preview)
      (val $draft ""))
    ; foces setting is impossible in iOS, so we border the field instead
    (if ios-detected
      (.addClass $draft "ui-border")
      (.focus $draft))))

; show the preview & publish buttons as soon as the user starts typing.
(.keyup $draft update-preview)

; when the publish button is clicked, compute the hash of the entered text and
; provided session key and assign to the field session-value;
; moreover, compute the password hash as md5 before transmission
(.click ($ :#publish-button)
        (fn [e]
          (do
            (.update md5 (val $plain-password))
            (val $plain-password nil)
            (when (val $plain-password)
              (val $password (crypt/byteArrayToHex (.digest md5))))
            (val ($ :#session-value) 
                 (lib/hash #(.charCodeAt % 0) (str (val $draft) (val ($ :#session-key))))))))
