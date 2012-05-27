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

(if-let [draft ($ "draft")]
  (focus/focusInputField draft))

; Show the Preview button as soon as the user starts typing.
(event/listen ($ "draft")
              :keypress
              (fn [e]
                (style/setStyle ($ "preview-button") "display" "block")))
