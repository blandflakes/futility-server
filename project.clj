(defproject futility-server "0.1.0-SNAPSHOT"
  :description "A simple server for hosting the futility analysis library"
  :url "https://f-utility.hms.harvard.edu/"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.cli "0.3.5"]
                 [com.google.code.gson/gson "2.6.2"]
                 [com.taoensso/nippy "2.11.1"]
                 [org.machinery.futility/futility-lib "1.0-SNAPSHOT"]
                 [ring/ring-core "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]]
  :main ^:skip-aot futility-server.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
