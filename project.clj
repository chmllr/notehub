(defproject NoteHub "2.0.0"
            :description "A free and anonymous hosting for markdown pages."
            :dependencies [[org.clojure/clojure "1.5.1"]
                           [hiccup "1.0.0"]
                           [ring/ring-core "1.1.0"]
                           [clj-redis "0.0.12"]
                           [noir "1.3.0-beta1"]]
            :jvm-opts ["-Dfile.encoding=utf-8"]
            :main NoteHub.server)
