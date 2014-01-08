(defproject load-path "0.0.1"
  :description "Load files with a sets of paths"
  :url "https://github.com/mylesmegyesi/load-path"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.cemerick/pomegranate "0.2.0"]]

  :profiles {:dev {:dependencies [[speclj "2.7.4"]]
                   :main speclj.main
                   :aot [speclj.main]
                   :plugins [[speclj "2.7.5"]]
                   :source-paths ["spec" "src"]
                   :test-paths ["spec"]
                   :target-path    "target/"
                   :resource-paths ["spec/resources1"
                                    "spec/resources2"]
                   :uberjar-name   "load-path-standalone.jar"}}

  )
