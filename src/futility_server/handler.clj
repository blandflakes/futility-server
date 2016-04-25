(ns futility-server.handler
  (:import [com.google.gson Gson]
           [java.io File ByteArrayOutputStream]
           [org.machinery.futility.analysis Algorithms])
  (:require [futility-server.session :refer
             [add-genome add-control add-experiment get-control get-experiment get-genome query-features
              remove-control remove-experiment remove-genome stored-data from-snapshot to-snapshot]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type header not-found redirect response]]
            [taoensso.nippy :as nippy]
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

; Begin query shit
(defmulti handle-query (fn [request] (get-in request [:params "queryType"])))

(defmethod handle-query "FEATURES"
  [request]
  (let [params (:params request)
        genome-name (get params "genomeName")
        gene-name (get params "geneName")]
    (json-response (query-features genome-name gene-name))))

(defmethod handle-query "GENOME"
  [request]
  (-> request
      :params
      (get "genomeName")
      get-genome
      json-response))

(defmethod handle-query "CONTROL_MEASUREMENTS"
  [request]
  (-> request
      :params
      (get "controlName")
      get-control
      .getSequenceMeasurements
      json-response))

(defmethod handle-query "EXPERIMENT_MEASUREMENTS"
  [request]
  (-> request
      :params
      (get "experimentName")
      get-experiment
      .getSequenceMeasurements
      json-response))

(defmethod handle-query "SESSION"
  [_]
  (json-response (stored-data)))

; Begin analysis shit
(defmulti handle-analysis (fn [request] (get-in request [:params "analysisType"])))

(defmethod handle-analysis "GENOME"
  [request]
  (let [params (:params request)
        name (get params "name")
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (add-genome (Algorithms/analyzeGenome name stream))
      (json-response (stored-data)))))

(defmethod handle-analysis "CONTROL"
  [request]
  (let [params (:params request)
        name (get params "name")
        genomeName (get params "genomeName")
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (add-control (Algorithms/analyzeControl name genomeName stream))
      (json-response (stored-data)))))


(defmethod handle-analysis "EXPERIMENT"
  [request]
  (let [params (:params request)
        name (get params "name")
        genome (get-genome (get params "genomeName"))
        control (get-control (get params "controlName"))
        input-file (get-in params ["file" :tempfile])]
    (with-open [stream (io/input-stream input-file)]
      (add-experiment (Algorithms/analyzeExperiment name genome control stream))
      (json-response (stored-data)))))

(defn handle-remove-data
  [request]
  (let [params (:params request)
        name (get params "name")
        type (get params "type")]
    (condp = type
      "GENOME" (remove-genome name)
      "CONTROL" (remove-control name)
      "EXPERIMENT" (remove-experiment name)))
  (json-response (stored-data)))

(defn handle-save-session
  []
  (let [temp-file (File/createTempFile "futility-session" "json")]
    (io/copy (nippy/freeze (to-snapshot)) temp-file)
    (json-response {"path" (.getAbsolutePath temp-file)})))

(defn handle-restore-session
  [request]
  (let [params (:params request)
        input-file (io/file (get params "path"))
        baos (ByteArrayOutputStream. (.length input-file))]
    (io/copy input-file baos)
    (from-snapshot (nippy/thaw (.toByteArray baos))))
  (json-response (stored-data)))

; Begin HTTP method handlers
(defmulti handler :request-method)

(defmethod handler :post [request]
  (condp = (:uri request)
    "/analyze" (handle-analysis request)
    "/remove-data" (handle-remove-data request)
    "/save-session" (handle-save-session)
    "/restore-session" (handle-restore-session request)
    (not-found-response (:uri request))))

(defmethod handler :get [request]
  (if (= (:uri request) "/query")
    (handle-query request)
    (not-found-response (:uri request))))

(defmethod handler :default [request]
  (not-found-response (:uri request)))

(def app
  (->
    handler
    (wrap-resource "public")
    wrap-content-type
    wrap-not-modified
    wrap-params
    wrap-multipart-params))