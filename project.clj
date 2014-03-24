(defproject NoteHub "2.0.0"
  :description "A free and anonymous hosting for markdown pages."
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [hiccup "1.0.0"]
                 [zeus "0.1.0"]
                 [cheshire "5.3.1"]
                 [ring/ring-core "1.2.0"]
                 [com.taoensso/carmine "2.4.4"]
                 [compojure "1.1.6"]]
  :min-lein-version "2.0.0"
  :plugins [[lein-ring "0.8.10"]]
  :ring {:handler notehub.handler/app}
  :profiles {:dev {:dependencies [[javax.servlet/servlet-api "2.5"]
                        [ring-mock "0.1.5"]]}}
  :jvm-opts ["-Dfile.encoding=utf-8"])
