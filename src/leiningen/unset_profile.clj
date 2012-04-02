(ns leiningen.unset-profile
  (:use configleaf.core)
  (:require [clojure.java.io :as io]
            [clojure.set :as set]))

(defn unset-profile
  "Set the profile(s) specified to be inactive. --all to remove all."
  [project & args]
  (let [flags (into #{} (map #(.toLowerCase (.substring % 2))
                             (filter #(.startsWith % "--") args)))
        args (filter #(not (.startsWith % "--")) args)
        current-profiles (into #{} (get-current-profiles))
        arg-profiles (into #{} (map keyword args))
        new-profiles (set/difference current-profiles arg-profiles)]
    (if (contains? flags "all")
      (do (save-current-profiles "." #{})
          (println "All profiles unset."))
      ;; No --all flag, so just unset the profiles mentioned.
      (do (save-current-profiles "." new-profiles)
          (print-current-sticky-profiles new-profiles)))))
