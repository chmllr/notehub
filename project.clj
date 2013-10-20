(defproject NoteHub "0.1.0-SNAPSHOT"
            :description "A free and anonymous hosting for markdown pages."
            :dependencies [[org.clojure/clojure "1.3.0"]
                           [org.clojure/clojure-contrib "1.2.0"]
                           [hiccup "1.0.0"]
                           [ring/ring-core "1.1.0"]
                           [cssgen "0.2.6"]
                           [cheshire "4.0.0"]
                           [clj-redis "0.0.12"]
                           [org.pegdown/pegdown "1.1.0"]
                           [noir "1.3.0-beta1"]]
            :plugins [[lein-cljsbuild "0.1.10"]]
            :hooks [leiningen.cljsbuild]
            :jvm-opts ["-Dfile.encoding=utf-8"]
            :cljsbuild
            {:crossovers [NoteHub.crossover],
             :builds
             [{:source-path "src-cljs",
               :compiler
               {:output-dir "resources/public/cljs/",
                :output-to "resources/public/cljs/main.js",
                :optimizations :whitespace,
                :pretty-print true}}]}
            :main NoteHub.server)

