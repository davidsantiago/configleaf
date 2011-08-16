(ns leiningen.hooks
  (:use configleaf.core
        leiningen.configleaf
        robert.hooke
        leiningen.core)
  (:require [leiningen.compile :as compile]))

;;
;; Leiningen hooks
;;

(defn setup-ns-hook
  "A hook to set up the namespace with the configuration before running a task."
  [task & args]
  (let [project (read-project)
        configleaf-data (:configleaf project)
        current-config (get-current-config configleaf-data)]
    (check-gitignore configleaf-data)
    (when (valid-configuration? configleaf-data current-config)
      (output-config-namespace configleaf-data
                               current-config))
    (apply task args)))

(defn setup-live-ns-hook
  "A hook for eval-in-project, so the ns is available for tasks that run
   in the project. Works by adding a call to setup-config-namespace to the
   init arg."
  [task & [project form & [handler skip-auto-compile init]]]
  (let [configleaf-data (:configleaf project)
        current-config (get-current-config configleaf-data)]
    (task project form handler skip-auto-compile
          `(do (require '~'leiningen.configleaf
                        '~'configleaf.core)
               (set-system-properties '~configleaf-data ~current-config)
               (setup-config-namespace '~configleaf-data ~current-config)
               ~init))))

(add-hook #'leiningen.core/apply-task setup-ns-hook)

(add-hook #'leiningen.compile/eval-in-project setup-live-ns-hook)