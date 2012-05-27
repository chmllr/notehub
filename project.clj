(defproject NoteHub "0.1.0-SNAPSHOT"
            :description "A free and anonymous hosting for markdown pages."
            :dependencies [[org.clojure/clojure "1.4.0"]
                           [hiccup "1.0.0"]
                           [cssgen "0.2.6"]
                           [org.pegdown/pegdown "1.1.0"]
                           [noir "1.3.0-beta1"]]
            :plugins [[lein-cljsbuild "0.1.10"]]
            :cljsbuild {
              :builds [{
                ; The path to the top-level ClojureScript source directory:
                :source-path "src-cljs"
                ; The standard ClojureScript compiler options:
                ; (See the ClojureScript compiler documentation for details.)
                :compiler {
                  :output-to "resources/public/js/main.js"  ; default: main.js in current directory
                  :optimizations :whitespace
                  :pretty-print true}}]}
            :main NoteHub.server)

