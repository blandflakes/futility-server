(ns futility-server.core
  (:require [ring.adapter.jetty :as jetty]
            [futility-server.handler :refer [app]])
  (:gen-class))

(defn -main
  [& args]
  (jetty/run-jetty app {:host "0.0.0.0" :port 5432}))

(-main)