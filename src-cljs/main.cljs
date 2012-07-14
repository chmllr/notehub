(ns NoteHub.main
  (:use [jayq.core :only [$ xhr css inner val anim show]])
  (:require [goog.dom :as gdom]
            [NoteHub.crossover.lib :as lib]
            [clojure.browser.dom :as dom]))

; frequently used selectors
(def $draft ($ :#draft))
(def $preview ($ :#preview))
(def $input-elems ($ :#input-elems))
(def $preview-start-line ($ :#preview-start-line))

; Markdown Converter & Sanitizer instantiation

(def md-converter (Markdown/getSanitizingConverter))

; try to detect iOS
(def ios-detected (.match (.-userAgent js/navigator) "(iPad|iPod|iPhone)"))

; set focus to the draft textarea (if there is one)
(when $draft
  (do
    (val $draft "")
    ; foces setting is impossible in iOS, so we border the field instead
    (if ios-detected
      (.addClass $draft "ui-border")
      (.focus $draft))))

; show the preview & publish buttons as soon as the user starts typing.
(.keyup $draft
           (fn [e]
             (do
               (show $preview-start-line)
               (show $input-elems)
               (inner $preview
                      (.makeHtml md-converter (val $draft))))))

; when the publish button is clicked, compute the hash of the entered text and
; provided session key and assign to the field session-value
(.click ($ :#publish-button)
        (fn [e]
          (val ($ :#session-value) 
               (lib/hash #(.charCodeAt % 0) (str (val $draft) (val ($ :#session-key)))))))
