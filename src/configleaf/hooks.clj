(ns configleaf.hooks
  (:use configleaf.core
        robert.hooke)
  (:require [clojure.string :as string]
            [leiningen.core.project :as project]
            leiningen.core.eval
            leiningen.show-profiles))

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
    (if (get-in configured-project [:configleaf :verbose])
      (println "Performing task" task-name "with profiles"
               (:included-profiles (meta configured-project))))
    (apply task task-name configured-project args)))

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
  (add-hook #'leiningen.show-profiles/show-profiles profiles-task-hook))