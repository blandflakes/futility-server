(ns futility-server.handler
  (:import [org.machinery.futility.analysis Algorithms]
           [org.machinery.futility.analysis.structs Genome])
  (:require [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [not-found redirect response]]
            [clojure.java.io :as io]))

(defmulti handle-analysis (fn [request] (get-in request [:params "analysisType"])))

(defmethod handle-analysis "GENOME"
  [request]
  (let [params (:params request)]
    (with-open [stream (io/input-stream (get-in params ["file" :tempfile]))]
      (Algorithms/analyzeGenome (get "name" params) stream))))

(defmethod handle-analysis "CONTROL"
  [request])

(defmethod handle-analysis "EXPERIMENT"
  [request])

(defmulti handler :request-method)

(defmethod handler :post [request]
  (handle-analysis request))

(defmethod handler :get [request]
  (if (= (:uri request) "/")
    (redirect "/index.html")
    (not-found (str (:uri request) " not found"))))

(def app
  (->
    handler
    (wrap-resource "public")
    wrap-content-type
    wrap-not-modified
    wrap-multipart-params))