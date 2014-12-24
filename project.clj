(defproject NoteHub "2.0.0"
            :description "A free and anonymous hosting for markdown pages."
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/core.cache "0.6.4"]
                           [hiccup "1.0.5"]
                           [zeus "0.1.0"]
                           [garden "1.2.5"]
                           [org.pegdown/pegdown "1.4.2"]
                           [iokv "0.1.1"]
                           [cheshire "5.3.1"]
                           [ring "1.3.1"]
                           [com.taoensso/carmine "2.7.0" :exclusions  [org.clojure/clojure]]
                           [compojure "1.2.0"]]
            :main notehub.handler
            :min-lein-version "2.0.0"
            :plugins [[lein-ring "0.8.12"]]
            :ring {:handler notehub.handler/app}
            :profiles {:uberjar {:aot :all}
                       :production {:ring {:auto-reload? false
                                           :auto-refresh? false}}
                       :dev {:ring {:auto-reload? true
                                    :auto-refresh? true}
                             :dependencies
                                   [[javax.servlet/servlet-api "2.5"]
                                    [ring-mock "0.1.5"]]}}
            :jvm-opts ["-Dfile.encoding=utf-8"])
