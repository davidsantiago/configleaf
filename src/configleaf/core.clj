(ns configleaf.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [stencil.core :as stencil])
  (:import java.util.Properties))

;;
;; Functions that provide functionality independent of any one build tool.
;;

(defn read-current-profile
  "Read the current profile from the config file for the project
   at the given path. For example, if the argument is \"/home/david\", reads
   \"/home/david/.configleaf/current\". .configleaf/current should contain a
   single form that names the current profile."
  [config-store-path]
  (let [config-file (io/file config-store-path ".configleaf/current")]
    (when (and (.exists config-file)
               (.isFile config-file))
      (read-string (slurp config-file)))))

(defn save-current-profile
  "Store the given profile as the new current profile for the project
   at the given path."
  [config-store-path current-profile]
  (let [config-dir (io/file config-store-path ".configleaf")
        config-file (io/file config-store-path ".configleaf/current")]
    (if (not (and (.exists config-dir)
                  (.isDirectory config-dir)))
      (.mkdir config-dir))
    (spit config-file (prn-str current-profile))))

(defn get-current-profile
  "Return the current profile (name) from the saved file. If no file,
   return the default profile. Otherwise, return nil."
  [cl-config]
  (or (read-current-profile ".")
      (:default cl-config)))

(defn valid-profile?
  "Returns true if profile is a profile listed in the cl-config data, where
   cl-config is the entire :configleaf data from the project.clj."
  [cl-config profile]
  (contains? (apply hash-set (keys (:profiles cl-config)))
             profile))

(defn config-namespace
  "Get the namespace to put the profile info into, or use the default
   (which is configleaf.current)."
  [cl-config]
  (or (:namespace cl-config)
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
  [cl-config current-profile-name]
  (let [cl-ns-name (symbol (config-namespace cl-config))
        current-profile (get-in cl-config [:profiles current-profile-name])
        current-profile-params (:params current-profile)]
    (create-ns cl-ns-name)
    (intern cl-ns-name 'profile-name current-profile-name)
    (intern cl-ns-name 'profile current-profile)
    (intern cl-ns-name 'params current-profile-params)))

(defn output-config-namespace
  "Write a Clojure file that will set up the config namespace and the values
   in it when it is loaded."
  [cl-config current-profile-name]
  (let [ns-name (str (config-namespace cl-config))
        ns-file (io/file "src/"
                         (str (string/replace ns-name
                                              #"[.-]"
                                              {"." "/", "-" "_"})
                              ".clj"))
        ns-parent-dir (.getParentFile ns-file)
        current-profile (get-in cl-config [:profiles current-profile-name])]
    (if (not (.exists ns-parent-dir))
      (.mkdirs ns-parent-dir))
    (spit ns-file (stencil/render-file "templates/configleafns"
                                       {:namespace ns-name
                                        :profile-name current-profile-name
                                        :profile current-profile
                                        :params (:params current-profile)}))))

(defn set-system-properties
  "Adds any properties from the current configuration's :java-properties key to
   the Java system properties. Both the key and value must be strings."
  [cl-config current-profile]
  (let [props (Properties. (System/getProperties))
        profile-properties (get-in cl-config [:profiles
                                              current-profile
                                              :java-properties])]
    (doseq [[k v] profile-properties]
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