(ns configleaf.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil]
            [leiningen.core.project :as project])
  (:import java.util.Properties))

;;
;; Functions that provide functionality independent of any one build tool.
;;

(defn read-current-profiles
  "Read the current profiles from the config file for the project
   at the given path. For example, if the argument is \"/home/david\", reads
   \"/home/david/.configleaf/current\". .configleaf/current should contain a
   single form that lists the currently active profiles."
  [config-store-path]
  (let [config-file (io/file config-store-path ".configleaf/current")]
    (when (and (.exists config-file)
               (.isFile config-file))
      (binding [*read-eval* false]
        (read-string (slurp config-file))))))

(defn save-current-profiles
  "Store the given profiles as the new current profiles for the project
   at the given path."
  [config-store-path current-profiles]
  (let [config-dir (io/file config-store-path ".configleaf")
        config-file (io/file config-store-path ".configleaf/current")]
    (if (not (and (.exists config-dir)
                  (.isDirectory config-dir)))
      (.mkdir config-dir))
    (spit config-file (prn-str current-profiles))))

(defn get-current-profiles
  "Return the current profiles (list of names) from the saved file.
   Otherwise, return nil."
  []
  (read-current-profiles "."))

(defn print-current-sticky-profiles
  "Given a set of profiles as an argument, print them in a standard way
   to stdout."
  [profiles]
  (if (not (empty? profiles))
    (println "Current sticky profiles:" profiles)
    (println "Current sticky profiles:")))

(defn valid-profile?
  "Returns true if profile is a profile listed in the project profiles."
  [project profile]
  (contains? (apply hash-set (keys (:profiles project)))
             profile))

(defn config-namespace
  "Get the namespace to put the profile info into, or use the default
   (which is cfg.current)."
  [project]
  (or (get-in project [:configleaf :namespace])
      'cfg.current))

(defn merge-profiles
  "Given the project map and a list of profile names, merge the profiles into
   the project map and return the result. Will check the :included-profiles
   metadata key to ensure that profiles are not merged multiple times."
  [project profiles]
  (let [included-profiles (into #{} (:included-profiles (meta project)))
        profiles-to-add (filter #(and (not (contains? included-profiles %))
                                      (contains? (:profiles project) %))
                                profiles)]
    (project/merge-profiles project profiles-to-add)))

(defn namespace-to-filepath
  "Given a namespace as a string/symbol, return a string containing the path
   where we'd look for its source."
  [ns-name]
  (str (string/replace ns-name
                       #"[.-]"
                       {"." "/", "-" "_"})
       ".clj"))

(defn require-config-namespace
  "Put the configured project map (according to given project) into
  a var in the config namespace (in the current JVM). Effect is as if
  you have 'require'd the namespace, but there is no clj file involved
  in the process.

  Note the argument is a string. You need to print the project map to a string
  to pass it into this function. This is so the string can survive leiningen 2's
  own macro handling, which does not prevent a project map from being evaluated,
  which you usually can't do, due to symbols and lists that would not eval."
  [project-as-str]
  (let [project (read-string project-as-str)
        cl-ns-name (symbol (config-namespace project))]
    (create-ns cl-ns-name)
    (intern cl-ns-name 'project project)))

(defn output-config-namespace
  "Write a Clojure file that will set up the config namespace with the project
   in it when it is loaded. Returns the project with the profiles merged."
  [project]
  (let [ns-name (str (config-namespace project))
        src-path (or (get-in project [:configleaf :config-source-path])
                     (first (:source-paths project))
                     "src/")
        ns-file (io/file src-path
                         (namespace-to-filepath ns-name))
        ns-parent-dir (.getParentFile ns-file)]
    (if (not (.exists ns-parent-dir))
      (.mkdirs ns-parent-dir))
    (spit ns-file (stencil/render-file "templates/configleafns"
                                       {:namespace ns-name
                                        :project project}))))

(defn set-system-properties
  "Given a map of string keys to string values, sets the Java properties named
   by the keys to have the value in the corresponding value."
  [properties]
  (let [props (Properties. (System/getProperties))]
    (doseq [[k v] properties]
      (try
        (.setProperty props k v)
        (catch Exception e
          (println "Configleaf")
          (println "  Java property keys and values must both be strings.")
          (println (str "  Skipping key: " k " value: " v)))))
    (System/setProperties props)))

(defn check-gitignore
  "Check the .gitignore file for the project to see if .configleaf/ is ignored,
   and warn if it isn't. Also check for the config-namespace source file."
  [cl-config]
  (try
    (let [gitignore-file (io/file ".gitignore")
          gitignore-lines (string/split-lines (slurp gitignore-file))
          ns-name (config-namespace cl-config)
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