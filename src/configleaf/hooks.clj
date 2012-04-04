(ns configleaf.hooks
  (:use configleaf.core
        robert.hooke)
  (:require [clojure.string :as string]
            [leiningen.core.project :as project]
            leiningen.core.eval
            leiningen.profiles))

;; We need leiningen-core on the classpath for these hooks, so we search the
;; classpath for leiningen-core's jar, regex-match its version, and add
;; that to the project map. Ugly, but automated.
(defn find-lein-core-on-classpath
  []
  (let [classpath (.getProperty (System/getProperties) "java.class.path")
        paths (string/split classpath #":")
        lein-core-re #"leiningen-core-(.*).jar"
        lein-core-jars (filter #(re-find lein-core-re
                                         %) paths)
        ;; Really shouldn't be multiple matching jars, take the first...
        lein-core-version (get (re-find lein-core-re (first lein-core-jars))
                               1)]
    (if (< 1 (count lein-core-jars))
      (println "Configleaf warning: Multiple leiningen-core jars found."))
    lein-core-version))

(defn find-configleaf-in-project
  "Given a project map, find the configleaf entry in the :plugins key, so we can
   figure out what version we are."
  [project]
  (last (filter #(or (= (first %) 'configleaf)
                     (= (first %) 'configleaf/configleaf))
                (:plugins project))))

(defn add-self-deps
  "Given a project map, inserts a dependency for leiningen-core and configleaf
   itself, so that tasks that run in the project can still run these hooks."
  [project]
  (let [lein-core-version (find-lein-core-on-classpath)
        configleaf-dep-entry (find-configleaf-in-project project)]
    (project/merge-profile project
                           {:dependencies [['leiningen-core
                                            lein-core-version]
                                           ['configleaf
                                            (second configleaf-dep-entry)]]})))

;;
;; Leiningen hooks
;;

(defn setup-ns-hook
  "A hook to set up the namespace with the configuration before running a task.
   Only meant to hook leiningen.core.main/apply-task."
  [task & [task-name project & args]]
  (let [configured-project (merge-profiles (add-self-deps project)
                                           (get-current-profiles))]
    (output-config-namespace configured-project)
    (if (get-in configured-project [:configleaf :verbose])
      (println "Performing task" task-name "with profiles"
               (:included-profiles (meta configured-project))))
    (apply task task-name configured-project args)))

(defn setup-live-ns-hook
  "A hook for eval-in-project, so the ns is available for tasks that run
   in the project. Works by adding a call to require-config-namespace to the
   init arg."
  [task & [project form init]]
  (let [configured-project (merge-profiles (add-self-deps project)
                                           (get-current-profiles))]
    (task configured-project form
          `(do (require 'configleaf.core)
               (require-config-namespace
                ~(pr-str configured-project)
                ~(pr-str (select-keys (meta configured-project)
                                      [:without-profiles :included-profiles])))
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