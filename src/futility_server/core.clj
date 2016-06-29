(ns futility-server.core
  (:require [clojure.string :as string]
            [clojure.tools.cli :as cli]
            [futility-server.handler :refer [app]]
            [futility-server.session :refer [init-session]]
            [ring.adapter.jetty :as jetty])
  (:gen-class)
  (:import (java.io File)))

(def options
  [["-p" "--port PORT" "port number that the server runs on"
    :default 9000
    :parse-fn #(Integer/parseInt %)]
   ["-d" "--directory DIRECTORY" "directory that will be used to persist analyzed data"
    :default (str "." (File/separator) "futility-session")]
   ["-h" "--help" "display this menu"]])

(defn- exit [status message]
  (println message)
  (System/exit status))

(defn- handle-errors [errors]
  (let [error-string (str " Unable to understand arguments: \n "
                          (string/join \newline errors))]
    (exit 1 error-string)))

(defn- handle-help [opts]
  (let [help-string (str " Futility is a tool for analyzing strains of antiobiotic- "
                         " resistant bacteria. Options: \n "
                         (:summary opts))]
    (exit 1 help-string)))

(defn- run-app [parsed-options]
  (init-session {:directory (:directory parsed-options)})
  (jetty/run-jetty app {:port (:port parsed-options)}))

(defn -main
  [& args]
  (let [opts (cli/parse-opts args options)]
    (cond
      (:errors opts) (handle-errors (:errors opts))
      (-> opts :options :help) (handle-help opts)
      :else (run-app (:options opts)))))