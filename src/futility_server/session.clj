(ns futility-server.session
  (:import (org.machinery.futility.analysis.structs Experiment Control Genome)))

; map of genome-name to Genome
(def genomes (atom {}))

; Map of experiment-name to Experiment
(def experiments (atom {}))

; Map of control-name to Control
(def controls (atom {}))

(defn add-genome
  [^Genome genome]
  (swap! genomes assoc (.getName genome) genome))

(defn add-control
  [^Control control]
  (swap! controls assoc (.getName control) control))

(defn add-experiment
  [^Experiment experiment]
  (swap! experiments assoc (.getName experiment) experiment))

(defn controls-with-genome
  [genome-name]
  (filter
    (fn [control]
      (= genome-name (.getGenomeName control)))
    (vals @controls)))

(defn experiments-with-control
  [control-name]
  (filter
    (fn [experiment]
      (= control-name (.getControlName experiment)))
    (vals @experiments)))

(defn dissoc-all
  [m args]
  (apply dissoc m args))

(defn remove-genome
  [^String genome-name]
  (let [controls-to-remove (controls-with-genome genome-name)
        control-names (map #(.getName %) controls-to-remove)
        experiments-to-remove (apply concat (map experiments-with-control control-names))
        experiment-names (map #(.getName %) experiments-to-remove)]
    (swap! genomes dissoc genome-name)
    (swap! controls dissoc-all control-names)
    (swap! experiments dissoc-all experiment-names)))

(defn remove-control
  [^String control-name]
  (let [experiments-to-remove (experiments-with-control control-name)
        experiment-names (map #(.getName %) experiments-to-remove)]
    (swap! experiments dissoc-all experiment-names)
    (swap! controls dissoc control-name)))

(defn remove-experiment
  [^String experiment-name]
  (swap! experiments dissoc experiment-name))

(defn get-features
  [^String gene-name ^Experiment experiment]
  (-> experiment
      .getGeneFeatureMeasurements
      (.get gene-name)))

(defn experiments-with-genome [^String genome-name]
  (filter
    (fn [^Experiment experiment]
      (= genome-name (.getGenomeName experiment)))
    (vals @experiments)))

; Returns a seq of GeneFeatureMeasurement objects
(defn query-features
  [^String genome-name ^String gene-name]
  (->> genome-name
    experiments-with-genome
    (map (partial get-features gene-name))))

(defn ^Genome get-genome
  [genome-name]
  (get @genomes genome-name))

(defn ^Control get-control
  [control-name]
  (get @controls control-name))

(defn ^Experiment get-experiment
  [experiment-name]
  (get @experiments experiment-name))

(defn control-entry
  [^Control control]
  {"name" (.getName control)
   "genomeName" (.getGenomeName control)})

(defn experiment-entry
  [^Experiment experiment]
  {"name" (.getName experiment)
   "controlName" (.getControlName experiment)
   "genomeName" (.getGenomeName experiment)})

(defn stored-data []
  {; genomes are just a set of names
   "genomes"     (or (keys @genomes) [])
   ; We need to link controls to their genomes
   "controls"    (reduce
                  (fn [acc ^Control control] (assoc acc (.getName control) (control-entry control)))
                  {}
                  (vals @controls))
   "experiments" (reduce
                  (fn [acc ^Experiment experiment] (assoc acc (.getName experiment) (experiment-entry experiment)))
                  {}
                  (vals @experiments))})

(defn to-snapshot
  []
  { :genomes @genomes
    :controls @controls
    :experiments @experiments })

(defn from-snapshot
  [snapshot]
  (when (:genomes snapshot)
    (reset! genomes (:genomes snapshot)))
  (when (:controls snapshot)
    (reset! controls (:controls snapshot)))
  (when (:experiments snapshot)
    (reset! experiments (:experiments snapshot))))