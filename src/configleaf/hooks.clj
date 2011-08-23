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
        cl-config (:configleaf project)
        current-profile (get-current-profile cl-config)]
    (check-gitignore cl-config)
    (when (valid-profile? cl-config current-profile)
      (output-config-namespace cl-config
                               current-profile))
    (apply task args)))

(defn setup-live-ns-hook
  "A hook for eval-in-project, so the ns is available for tasks that run
   in the project. Works by adding a call to setup-config-namespace to the
   init arg."
  [task & [project form & [handler skip-auto-compile init]]]
  (let [cl-config (:configleaf project)
        current-profile (get-current-profile cl-config)]
    (task project form handler skip-auto-compile
          `(do (require '~'leiningen.configleaf
                        '~'configleaf.core)
               (set-system-properties '~cl-config ~current-profile)
               (setup-config-namespace '~cl-config ~current-profile)
               ~init))))

(add-hook #'leiningen.core/apply-task setup-ns-hook)

(add-hook #'leiningen.compile/eval-in-project setup-live-ns-hook)