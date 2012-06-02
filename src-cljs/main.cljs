(ns NoteHub.main
  (:use [jayq.core :only [$ css inner val anim show]])
  (:require [fetch.remotes :as remotes]
            [goog.dom :as gdom]
            [NoteHub.crossover.lib :as nh]
            [clojure.browser.dom :as dom]
            [clojure.browser.event :as event])
  (:require-macros [fetch.macros :as fm]))

; frequently used selectors
(def $draft ($ :#draft))
(def $preview ($ :#preview))
(def $session-key ($ :#session-key))
(def $preview-start-line ($ :#preview-start-line))

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
            (fm/remote (get-preview-md (val $session-key) (val $draft)) [{:keys [preview session-key]}] 
                       (show $preview-start-line)
                       (inner $preview preview)
                       (val $session-key session-key)
                       (scroll-to $preview-start-line)))))

(.click ($ :#publish-button)
        (fn [e]
          (val ($ :#session-value) 
               (nh/hash #(.charCodeAt % 0) (str (val $draft) (val $session-key))))))
