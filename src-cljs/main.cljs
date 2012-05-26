(ns NoteHub.main
  (:require [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [goog.style :as style]
            [goog.dom.classes :as classes]
            [clojure.browser.event :as event]
            [goog.editor.focus :as focus]))

(defn log [obj]
  (.log js/console obj))

(defn $ [selector]
  (let [type (first selector) name (apply str (rest selector))]
    (cond (= \# type) (dom/get-element name)
          (= \. type) (gdom/getElementByClass name))))

(if-let [write-textarea ($ "#write-textarea")]
  (focus/focusInputField write-textarea))

(defn show-form-buttons []
  (style/setStyle ($ ".form-button") "display" "block"))

(event/listen ($ "#write-textarea")
              :keypress
              (fn [e]
                (show-form-buttons)))

