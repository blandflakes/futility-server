(ns futility-server.handler
  (:import [com.google.gson Gson]
           [org.machinery.futility.analysis Algorithms]
           [org.machinery.futility.analysis.structs Genome Control])
  (:require [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type header not-found redirect response]]
            [clojure.java.io :as io]))

(defonce gson (new Gson))

(defn json-response [item]
  (->
    (->> item
         (.toJson gson)
         response)
    (header "Content-Type" "application/json")))

(defn not-found-response
  [uri]
  (-> (not-found (str uri " not found"))
      (content-type "text")))

(defmulti handle-analysis (fn [request] (get-in request [:params "analysisType"])))

(defmethod handle-analysis "GENOME"
  [request]
  (let [params (:params request)
        name (get params "name")
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (json-response (Algorithms/analyzeGenome name stream)))))

(defmethod handle-analysis "CONTROL"
  [request]
  (let [params (:params request)
        name (get params "name")
        genomeName (get params "genomeName")
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (json-response (Algorithms/analyzeControl name genomeName stream)))))

(defmethod handle-analysis "EXPERIMENT"
  [request]
  (let [params (:params request)
        name (get params "name")
        genome (.fromJson gson (get params "genome") Genome)
        control (.fromJson gson (get params "control") Control)
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (json-response (Algorithms/analyzeExperiment name genome control stream)))))

(defmulti handler :request-method)

(defmethod handler :post [request]
  (if (= (:uri request) "/analyze")
    (handle-analysis request)
    (not-found-response (:uri request))))

(defmethod handler :default [request]
  (not-found-response (:uri request)))

(def app
  (->
    handler
    (wrap-resource "public")
    wrap-content-type
    wrap-not-modified
    wrap-multipart-params))