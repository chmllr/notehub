(ns NoteHub.main
  (:use [jayq.core :only [$ css inner val anim]])
  (:require [fetch.remotes :as remotes]
            [clojure.browser.dom :as dom]
            [clojure.browser.event :as event])
  (:require-macros [fetch.macros :as fm]))

; frequently used selectors
(def $draft ($ :#draft))
(def $preview ($ :#preview))

(defn scroll-to 
  "scrolls to the given selector"
  [$id]
  (anim ($ :body) {:scrollTop ((js->clj (.offset $id)) "top")} 500))

; set focus to the draft textarea (if there is one)
(.focus $draft)

; show the preview & publish buttons as soon as the user starts typing.
(.keypress $draft
           (fn [e]
             (css ($ :#buttons) {:display :block})))

; on a preview button click, transform markdown to html, put it 
; to the preview layer and scroll to it
(.click ($ :#preview-button)
        (fn [e]
          (do
            (fm/remote (md-to-html (val $draft)) [result] 
                       (inner $preview result)
                       (scroll-to $preview)))))
