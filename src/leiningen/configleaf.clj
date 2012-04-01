(ns leiningen.configleaf
  (:use configleaf.core)
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [leiningen.core.project :as project]))

;;
;; Command handlers
;;

(defn print-help
  []
  (println "Configleaf commands:")
  (println "  help       - This command.")
  (println "  set-profiles - Set the current configuration.")
  (println "  status     - Display the current configuration."))

(defn print-current-profiles
  [cl-config]
  (let [current-profiles (get-current-profiles)]
    (println "Configleaf")
    (if current-profiles
      (println "  Current configuration: " current-profiles)
      (do
        (println "  No current configuration. Set one with set-profiles or")
        (println "  add a default.")))))

(defn set-current-profiles
  "Write the given profiles as the current profiles."
  [project new-profiles]
  (save-current-profiles "." new-profiles)
  (do (println "Configleaf")
      (println "  Current profiles:" new-profiles)))

(defn configleaf
  "Main entry point for explicitly issued plugin tasks."
  [project & [task & args]]
  (let [cl-config (:configleaf project)]
    (case task
          "status" (print-current-profiles cl-config)
          "set-profiles" (set-current-profiles project
                                               (map keyword args))
          (print-help))))