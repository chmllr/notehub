(ns NoteHub.main
  (:require [clojure.browser.dom :as dom]))

(if-let [write-textarea (dom/get-element "write-textarea")]
  (dom/append write-textarea "test"))

