(ns NoteHub.main
  (:require [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [goog.style :as style]
            [goog.dom.classes :as classes]
            [clojure.browser.event :as event]
            [goog.editor.focus :as focus]))

(defn $ 
  "The DOM-element selector."
  [selector]
    (dom/get-element selector))

(if-let [write-textarea ($ "write-textarea")]
  (focus/focusInputField write-textarea))

; Show the Preview button as soon as the user starts typing.
(event/listen ($ "write-textarea")
              :keypress
              (fn [e]
                (style/setStyle ($ "form-button") "display" "block")))
