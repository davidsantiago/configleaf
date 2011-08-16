(ns configleaf.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil])
  (:import java.util.Properties))

;;
;; Functions that provide functionality independent of any one build tool.
;;

(defn read-current-config
  "Read the current configuration from the config file for the project
   at the given path. For example, if the argument is \"/home/david\", reads
   \"/home/david/.configleaf/current\". .configleaf/current should contain a
   single form that names the current configuration."
  [config-store-path]
  (let [config-file (io/file config-store-path ".configleaf/current")]
    (when (and (.exists config-file)
               (.isFile config-file))
      (read-string (slurp config-file)))))

(defn save-current-config
  "Store the given configuration as the new current config in for the project
   at the given path."
  [config-store-path current-config]
  (let [config-dir (io/file config-store-path ".configleaf")
        config-file (io/file config-store-path ".configleaf/current")]
    (if (not (and (.exists config-dir)
                  (.isDirectory config-dir)))
      (.mkdir config-dir))
    (spit config-file (prn-str current-config))))

(defn get-current-config
  "Return the current configuration from the saved file. If no file,
   return the default configuration. Otherwise, return nil."
  [configleaf-data]
  (or (read-current-config ".")
      (:default configleaf-data)))

(defn valid-configuration?
  "Returns true if cfg is a configuration listed in the configleaf-data."
  [configleaf-data cfg]
  (contains? (apply hash-set (keys (:configurations configleaf-data)))
             cfg))

(defn config-namespace
  "Get the namespace to put the configuration info into, or use the default
   (which is configleaf.current)."
  [configleaf-data]
  (or (:namespace configleaf-data)
      'configleaf.current))

(defn namespace-to-filepath
  "Given a namespace as a string/symbol, return a string containing the path
   where we'd look for its source."
  [ns-name]
  (str (string/replace ns-name
                       #"[.-]"
                       {"." "/", "-" "_"})
       ".clj"))

(defn setup-config-namespace
  "Put the configured values into vars in the config namespace
   (in the current JVM)."
  [configleaf-data current-cfg]
  (let [cl-ns-name (symbol (config-namespace configleaf-data))
        current-cfg-data (get-in configleaf-data [:configurations
                                                  current-cfg
                                                  :data])]
    (create-ns cl-ns-name)
    (intern cl-ns-name 'config-data current-cfg-data)
    (intern cl-ns-name 'config current-cfg)))

(defn output-config-namespace
  "Write a Clojure file that will set up the config namespace and the values
   in it when it is loaded."
  [configleaf-data current-cfg]
  (let [ns-name (str (config-namespace configleaf-data))
        ns-file (io/file "src/"
                         (str (string/replace ns-name
                                              #"[.-]"
                                              {"." "/", "-" "_"})
                              ".clj"))
        ns-parent-dir (.getParentFile ns-file)]
    (if (not (.exists ns-parent-dir))
      (.mkdirs ns-parent-dir))
    (spit ns-file (stencil/render-file "templates/configleafns"
                                       {:namespace ns-name
                                        :config current-cfg
                                        :config-data (get-in configleaf-data
                                                             [:configurations
                                                              current-cfg
                                                              :data])}))))

(defn set-system-properties
  "Adds any properties from the current configuration's :properties key to
   the Java system properties. Both the key and value must be strings."
  [configleaf-data current-cfg]
  (let [props (Properties. (System/getProperties))
        config-properties (get-in configleaf-data [:configurations
                                                   current-cfg
                                                   :properties])]
    (doseq [[k v] config-properties]
      (try
        (.setProperty props k v)
        (catch Exception e
          (println "Configleaf")
          (println "  Properties keys and values must both be strings.")
          (println (str "  Skipping key: " k " value: " v)))))
    (System/setProperties props)))

(defn check-gitignore
  "Check the .gitignore file for the project to see if .configleaf/ is ignored,
   and warn if it isn't. Also check for the config-namespace source file."
  [configleaf-data]
  (try
    (let [gitignore-file (io/file ".gitignore")
          gitignore-lines (string/split-lines (slurp gitignore-file))
          ns-name (config-namespace configleaf-data)
          ns-filepath (namespace-to-filepath ns-name)
          config-ns-re (re-pattern (str "\\Qsrc/" ;; Quote whole thing.
                                        ns-filepath
                                        "\\E"))]
      (when (not (some #(re-matches #"\.configleaf" %) gitignore-lines))
        (println "Configleaf")
        (println "  The .configleaf directory does not appear to be present in")
        (println "  .gitignore. You almost certainly want to add it."))
      (when (not (some #(re-matches config-ns-re %) gitignore-lines))
        (println "Configleaf")
        (println "  The Configleaf namespace file," (str "src/" ns-filepath)
                 ", does not")
        (println "  appear to be present in .gitignore. You almost certainly")
        (println "  want to add it.")))
    ;; For now, just be quiet if we can't open the file.
    (catch Exception e)))