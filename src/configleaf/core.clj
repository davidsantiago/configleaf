(ns configleaf.core
  (:require [clojure.java.io :as io]))

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

