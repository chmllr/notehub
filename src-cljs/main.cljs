(ns NoteHub.main
  (:use [jayq.core :only [$ xhr css inner val anim show]])
  (:require [goog.dom :as gdom]
            [NoteHub.crossover.lib :as lib]
            [clojure.browser.dom :as dom]
            [clojure.browser.event :as event]))

; frequently used selectors
(def $draft ($ :#draft))
(def $preview ($ :#preview))
(def $session-key ($ :#session-key))
(def $preview-start-line ($ :#preview-start-line))

(defn scroll-to 
  "scrolls to the given selector"
  [$id]
  (anim ($ :body) {:scrollTop ((js->clj (.offset $id)) "top")} 500))

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
(.keypress $draft
           (fn [e]
             (css ($ :#buttons) {:display :block})))

; on a preview button click, transform markdown to html, put it 
; to the preview layer and scroll to it
(.click ($ :#preview-button)
        (fn [e]
          (xhr [:post "/preview"]
               {:draft (val $draft)}
               (fn [json-map]
                 (let [m (js->clj (JSON/parse json-map))]
                   (do
                     (inner $preview (m "preview"))
                     (show $preview-start-line)
                     (scroll-to $preview-start-line)))))))

; when the publish button is clicked, compute the hash of the entered text and
; provided session key and assign to the field session-value
(.click ($ :#publish-button)
        (fn [e]
          (val ($ :#session-value) 
               (lib/hash #(.charCodeAt % 0) (str (val $draft) (val $session-key))))))
