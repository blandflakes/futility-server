(ns futility-server.session
  (:import [com.google.gson Gson]
           [org.machinery.futility.analysis.structs Experiment Control Genome SequenceMeasurements]
           [java.io File])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futility-server.features.store :as store]))

; Should probably do some passing around of config, but it's actually... not trivial in Clojure
; to pass configs to the right places. For now, we'll set this on startup from the server, and
; read it in this namespace
; This is absolutely the wrong way to do this, but I just don't want to spend time focusing on config right now.
(def session-config (atom {}))

; atoms for keeping track of the things we've analyzed
(def genomes (atom {}))
(def controls (atom {}))
(def experiments (atom {}))

; We use gson to write out the analyzed stuff because they're java classes
(def gson (new Gson))

(defn stored-data
  "Returns a listing of the current analyzed data for selection by the client."
  []
  {"genomes" @genomes
   "controls" @controls
   "experiments" @experiments})

(defn- ^String filepath
  "Returns a string representing a file path constructed from the passed-in parts."
  [& parts]
  (str/join (File/separator) parts))

(defn- session-path
  "Provides the path that the session should exist at, based on the configured directory"
  []
  (filepath (:directory @session-config) "session.edn"))

(defn- save-session
  "Persists the session's state to the configured session directory."
  []
  (spit (session-path) (prn-str (stored-data))))

(defn- hydrate-session
  "Restores the session from the configured session directory, if it exists."
  []
  (when (.exists (io/as-file (session-path)))
    (let [parsed (read-string (slurp (session-path)))]
      (reset! genomes (get parsed "genomes"))
      (reset! controls (get parsed "controls"))
      (reset! experiments (get parsed "experiments")))))

(defn- setup-directories
  "Sets up the directories under the configured subdirectory for storing analyzed data."
  []
  (let [directory (:directory @session-config)]
    (.mkdirs (File. (filepath directory "genomes")))
    (.mkdirs (File. (filepath directory "measurements" "controls")))
    (.mkdirs (File. (filepath directory "measurements" "experiments")))))

(defn init-session
  "Initializes the session with the provided config. This means saving the config (which contains the directory for
  persisting analyzed data) and restoring the session if one previously existed."
  [config]
  (reset! session-config config)
  ; directories must be set up first, so let's handle our session before the store
  (if (.exists (io/as-file (session-path)))
    (hydrate-session)
    (setup-directories))
  (store/initialize config))

(defn- control-entry
  "Returns an entry tracking the dependencies of an analyzed control. We must know the genome that the control is
  associated with in order to remove it later."
  [^Control control]
  {"name" (.getName control)
   "genomeName" (.getGenomeName control)})

(defn- experiment-entry
  "Returns an entry tracking the dependencies of an analyzed experiment. Experiments are linked to both controls
  and the genome they were run on."
  [^Experiment experiment]
  {"name" (.getName experiment)
   "controlName" (.getControlName experiment)
   "genomeName" (.getGenomeName experiment)})

(defn genome-file-path
  "Returns the path where the genome mapping file for a provided genome should be."
  [genome-name]
  (filepath (:directory @session-config) "genomes" (str genome-name ".json")))

(defn measurements-file-path
  "Returns the path where the sequence measurements for a given control or experiment should be.
  type is either 'control' or 'experiment'."
  [^String name ^String type]
  (filepath (:directory @session-config) "measurements" type (str name ".json")))

(defn add-genome
  "Adds a genome to the analyzed data set. This entails persisting the genome to the 'genomes' directory as JSON
  and updating the session's state."
  [^Genome genome]
  (let [name (.getName genome)
        path (genome-file-path name)]
    (spit path (.toJson gson genome))
    (swap! genomes assoc name {"name" name})
    (save-session)))

(defn ^Genome hydrate-genome
  "Deserializes a stored genome, which is used when an analysis needs to be run."
  [^String name]
  (let [json-string (-> name genome-file-path slurp)]
    (.fromJson gson json-string Genome)))

(defn add-control
  "Adds a control to the analyzed data set. We update the session's state with links to the control's genome,
  and write out the sequence measurements for the control to the 'measurements/controls' directory."
  [^Control control]
  (let [name (.getName control)
        path (measurements-file-path name "controls")]
    (spit path (.toJson gson (.getSequenceMeasurements control)))
    (swap! controls assoc name (control-entry control))
    (save-session)))

(defn ^Control hydrate-control
  "Deserializes a stored control. The mappings are easily queried for the visualizer, but when we analyze an
  experiment, we want to pass a well-typed Control object to the Java code. We rejoin the sequence measurements
  with the control metadata (genome nameand control name)."
  [^String name]
  (let [metadata (get @controls name)
        json-string (-> name (measurements-file-path "controls") slurp)
        ^SequenceMeasurements measurements (.fromJson gson json-string SequenceMeasurements)]
    (Control. (get metadata "name") (get metadata "genomeName") measurements)))

(defn add-experiment
  "Adds an experiment to the analyzed data set. This is the most complicated ingestion, because to do things any sort
  of efficiently, we can't persist the experiment wholesale. The sequence measurements will be written out like
  those of a control to the 'measurements/experiments' directory. The features, however, will be persisted to
  a database, so that we don't have to load each experiment's features every time the fitness table is updated
  (we can select a single gene instead). There is not yet a use case for rehydrating an experiment, so I haven't
  implemented hydrate-experiment."
  [^Experiment experiment]
  (let [name (.getName experiment)
        path (measurements-file-path name "experiments")]
    (spit path (.toJson gson (.getSequenceMeasurements experiment)))
    (store/add-features (.getGenomeName experiment) (.getGeneFeatureMeasurements experiment))
    (swap! experiments assoc name (experiment-entry experiment))
    (save-session)))

(defn- controls-with-genome
  "Helper function. Finds all controls that correspond to a given genome."
  [genome-name]
  (filter
    (fn [control-record]
      (= genome-name (get control-record "genomeName")))
    (vals @controls)))

(defn- experiments-with-control
  "Helper function. Finds all experiments that correspond to a given control."
  [control-name]
  (filter
    (fn [experiment-record]
      (= control-name (get experiment-record "controlName")))
    (vals @experiments)))

(defn- dissoc-all
  "Simple helper function for dissociating a seq of things without having to use apply inline."
  [m args]
  (apply dissoc m args))

; These internal delete methods could be written by macros or a function the generates functions...
; They do the exact same thing.
(defn- delete-genomes
  [^String genome-names]
  (swap! genomes dissoc-all genome-names)
  (doseq [genome-name genome-names]
    (io/delete-file (genome-file-path genome-name) true)))

(defn- delete-experiments
  "Helper function. Deletes the listed experiments from the session's state and from disk."
  [^String experiment-names]
  (swap! experiments dissoc-all experiment-names)
  (doseq [experiment-name experiment-names]
    (store/delete-features experiment-name)
    (io/delete-file (measurements-file-path experiment-name "experiments") true)))

(defn- delete-controls
  "Helper function. Deletes the provided controls from the session's state and from disk"
  [^String control-names]
  (swap! controls dissoc-all control-names)
  (doseq [control-name control-names]
    (io/delete-file (measurements-file-path control-name "controls") true)))

(defn remove-genome
  "Removes a genome from the analyzed data. Will remove all data that was analyzed using this genome."
  [^String genome-name]
  (let [controls-to-remove (controls-with-genome genome-name)
        control-names (map #(get % "name") controls-to-remove)
        experiments-to-remove (apply concat (map experiments-with-control control-names))
        experiment-names (map #(get % "name") experiments-to-remove)]
    (delete-genomes [genome-name])
    (delete-controls control-names)
    (delete-experiments experiment-names)
    (save-session)))

(defn remove-control
  "Removes a control from the analyzed data. Will remove all experiments analyzed with this control."
  [^String control-name]
  (let [experiments-to-remove (experiments-with-control control-name)
        experiment-names (map #(get % "name") experiments-to-remove)]
    (delete-experiments experiment-names)
    (delete-controls [control-name])
    (save-session)))

(defn remove-experiment
  "Removes an experiment from the analyzed data."
  [^String experiment-name]
  (delete-experiments [experiment-name])
  (save-session))

; Returns a seq of GeneFeatureMeasurement objects
(defn query-features
  [^String genome-name ^String gene-name]
  (store/features genome-name gene-name))

(defn clear-session
  "Erases all data stored in the session."
  []
  (delete-genomes (keys @genomes))
  (delete-controls (keys @controls))
  (delete-experiments (keys @experiments))
  (save-session))