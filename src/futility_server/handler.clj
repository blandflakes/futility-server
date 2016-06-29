(ns futility-server.handler
  (:import [com.google.gson Gson]
           [org.machinery.futility.analysis Algorithms])
  (:require [futility-server.session :refer
             [add-genome add-control add-experiment clear-session genome-file-path hydrate-control hydrate-genome
              query-features measurements-file-path remove-control remove-experiment remove-genome stored-data]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.not-modified :refer [wrap-not-modified]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.util.response :refer [content-type file-response header not-found redirect response]]
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
      genome-file-path
      file-response))

(defmethod handle-query "CONTROL_MEASUREMENTS"
  [request]
  (-> request
      :params
      (get "controlName")
      (measurements-file-path "controls")
      file-response))

(defmethod handle-query "EXPERIMENT_MEASUREMENTS"
  [request]
  (-> request
      :params
      (get "experimentName")
      (measurements-file-path "experiments")
      file-response))

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
        genome (hydrate-genome (get params "genomeName"))
        control (hydrate-control (get params "controlName"))
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

(defn handle-clear-session
  []
  (clear-session)
  (json-response (stored-data)))

; Begin HTTP method handlers
(defmulti handler :request-method)

(defmethod handler :post [request]
  (condp = (:uri request)
    "/analyze" (handle-analysis request)
    "/remove-data" (handle-remove-data request)
    "/clear-session" (handle-clear-session)
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