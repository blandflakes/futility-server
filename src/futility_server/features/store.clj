(ns futility-server.features.store
  (:require [yesql.core :refer [defquery defqueries]])
  (:import (org.machinery.futility.analysis.structs GeneFeatureMeasurements)))

(defquery create-features-table! "queries/features_table.sql")
(defqueries "queries/features.sql")

(def db (atom {}))

(defn initialize
  [app-config]
  (reset! db {:classname "org.sqlite.JDBC"
              :subprotocol "sqlite"
              :subname (-> app-config :directory (str "/features.db"))})
  (create-features-table! {} {:connection @db}))

(defn features [^String genome-name ^String gene-name]
  (select-features {:genome_name genome-name :gene_name gene-name} {:connection @db}))

(defn delete-features [condition-name]
  (delete-features! {:condition condition-name} {:connection @db}))

; feature-map is a HashMap because I'm an idiot.
(defn add-features [^String genome-name feature-map]
  (doseq [gene-name (.keySet feature-map)]
    (let [^GeneFeatureMeasurements featureMeasurement (.get feature-map gene-name)]
      (insert-feature! {:genome_name genome-name
                        :gene_name gene-name :condition (.getCondition featureMeasurement)
                        :num_ta_sites (.getNumTASites featureMeasurement)
                        :gene_length (.getGeneLength featureMeasurement)
                        :num_control_reads (.getNumControlReads featureMeasurement)
                        :num_experiment_reads (.getNumExperimentReads featureMeasurement)
                        :modified_ratio (.getModifiedRatio featureMeasurement)
                        :p (.getP featureMeasurement)
                        :essentiality_index (.getEssentialityIndex featureMeasurement)
                        :fitness (.getFitness featureMeasurement)}
                       {:connection @db}))))