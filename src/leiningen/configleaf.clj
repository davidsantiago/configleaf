(ns leiningen.configleaf
  (:use configleaf.core)
  (:require [clojure.java.io :as io]))

;;
;; Command handlers
;;

(defn print-help
  []
  (println "Configleaf commands:")
  (println "  help       - This command.")
  (println "  set-config - Set the current configuration.")
  (println "  status     - Display the current configuration."))

(defn print-current-config
  [configleaf-data]
  (let [current-cfg (get-current-config configleaf-data)]
    (println "Configleaf")
    (if current-cfg
      (println "  Current configuration: " current-cfg)
      (do
        (println "  No current configuration. Set one with set-config or")
        (println "  add a default.")))))

(defn set-current-config
  "Check if the given configuration is in the :configurations key of the
   configleaf data and if so, write that as the current config."
  [configleaf-data new-config]
  (if (valid-configuration? configleaf-data new-config)
    (save-current-config "." new-config)
    (do (println "Configleaf")
        (println "  The given configuration " new-config " does not exist."))))

(defn configleaf
  "Main entry point for explicitly issued plugin tasks."
  [project & [task & args]]
  (let [configleaf-data (:configleaf project)]
    (case task
          "status" (print-current-config configleaf-data)
          "set-config" (set-current-config configleaf-data
                                           (read-string (first args)))
          (print-help))))

