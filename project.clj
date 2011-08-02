(defproject configleaf "0.1.0-SNAPSHOT"
  :description "Software configuration for Clojure"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [stencil "0.1.2"]
                 [robert/hooke "1.1.2"]]
  :dev-dependencies [[ritz "0.1.7"]]

  ;  :eval-in-leiningen true
  :hooks [leiningen.configleaf]

  :configleaf {:configurations {:test {}}
               :namespace test.testing})