(ns configleaf.hooks
  (:use configleaf.core
        robert.hooke)
  (:require leiningen.core.project
            leiningen.core.eval
            leiningen.profiles))

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
               (require-config-namespace ~(pr-str configured-project))
               ~init))))

(defn profiles-task-hook
  "This is a hook for the profiles task, so it will print the current sticky
   profiles after it does whatever it does."
  [task & args]
  (apply task args)
  ;; Only print current profiles when no args on command line (first arg is
  ;; project).
  (when (== 1 (count args))
    (println "")
    (print-current-sticky-profiles (get-current-profiles))))

(defn activate
  []
  (add-hook #'leiningen.core.main/apply-task setup-ns-hook)
  (add-hook #'leiningen.core.eval/eval-in-project setup-live-ns-hook)
  (add-hook #'leiningen.profiles/profiles profiles-task-hook))