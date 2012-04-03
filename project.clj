(defproject configleaf "0.4.1"
  :description "Persistent and buildable profiles in Leiningen."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [stencil "0.1.2"]
                 [robert/hooke "1.1.2"]]

  :eval-in-leiningen true
  :hooks [configleaf.hooks]

  :profiles {:test {:params {:test 1}
                    :java-properties {"test" "1"
                                      :test2 "2"}}})