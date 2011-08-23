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

(defn print-current-profile
  [cl-config]
  (let [current-profile (get-current-profile cl-config)]
    (println "Configleaf")
    (if current-profile
      (println "  Current configuration: " current-profile)
      (do
        (println "  No current configuration. Set one with set-config or")
        (println "  add a default.")))))

(defn set-current-profile
  "Check if the given profile is in the :profiles key of the
   configleaf data and if so, write that as the current profile."
  [cl-config new-profile]
  (if (valid-profile? cl-config new-profile)
    (save-current-profile "." new-profile)
    (do (println "Configleaf")
        (println "  The given configuration " new-profile " does not exist."))))

(defn configleaf
  "Main entry point for explicitly issued plugin tasks."
  [project & [task & args]]
  (let [cl-config (:configleaf project)]
    (case task
          "status" (print-current-profile cl-config)
          "set-profile" (set-current-profile cl-config
                                             (read-string (first args)))
          (print-help))))

