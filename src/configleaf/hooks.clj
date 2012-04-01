(ns configleaf.hooks
  (:use configleaf.core
        leiningen.configleaf
        robert.hooke)
  (:require [leiningen.core.project :as project]
            [leiningen.core.eval :as eval]))

;;
;; Leiningen hooks
;;

(defn setup-ns-hook
  "A hook to set up the namespace with the configuration before running a task.
   Only meant to hook leiningen.core.main/apply-task."
  [task & [task-name project & args]]
  (let [configured-project (merge-profiles project
                                           (get-current-profiles))]
    (output-config-namespace configured-project)
    (apply task task-name configured-project args)))

(defn setup-live-ns-hook
  "A hook for eval-in-project, so the ns is available for tasks that run
   in the project. Works by adding a call to require-config-namespace to the
   init arg."
  [task & [project form init]]
  (let [configured-project (merge-profiles project (get-current-profiles))
        blah configured-project]
    (task configured-project form
          `(do (require 'configleaf.core)
               (set-system-properties ~(:java-properties configured-project))
               (require-config-namespace ~(pr-str configured-project))
               ~init))))

(defn activate
  []
  (add-hook #'leiningen.core.main/apply-task setup-ns-hook)
  (add-hook #'leiningen.core.eval/eval-in-project setup-live-ns-hook))