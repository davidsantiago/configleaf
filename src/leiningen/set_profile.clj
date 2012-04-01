(ns leiningen.set-profile
  (:use configleaf.core)
  (:require [clojure.set :as set]))

(defn set-profile
  "Set the profile(s) specified to be active."
  [project & args]
  (let [arg-profiles (into #{} (map keyword args))
        current-profiles (into #{} (get-current-profiles))
        project-profiles (into #{} (keys (:profiles project)))
        known-profiles (set/union project-profiles #{:default :dev :test :user})
        new-profiles (set/union current-profiles arg-profiles)]
    ;; Print a warning for the unknown profiles, but set them anyways.
    (doseq [unknown-profile (filter #(not (contains? known-profiles %))
                                    new-profiles)]
      (println "Warning: Unknown profile" unknown-profile
               "is being set active."))
    ;; Check if task was called without args, before saving; calling without
    ;; args should not change anything.
    (if (not (empty? arg-profiles))
      (save-current-profiles "." new-profiles))
    (print-current-profiles new-profiles)))
