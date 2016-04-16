(ns futility-server.core
  (:require [ring.adapter.jetty :as jetty]
            [futility-server.handler :refer [app]])
  (:gen-class))

(defn get-port
  [args]
  (if args
    (Integer/parseInt (first args))
    6666))

(defn -main
  [& args]
  (let [port (get-port args)]
    (jetty/run-jetty app {:port port})))