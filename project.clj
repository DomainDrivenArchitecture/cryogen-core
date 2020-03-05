(defproject dda/cryogen-core "0.2.1"
  :description "Cryogen's compiler"
  :url "https://github.com/cryogen-project/cryogen-core"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [camel-snake-kebab "0.4.1"]
                 [cheshire "5.10.0"]
                 [clj-rss "0.2.5"]
                 [clj-text-decoration "0.0.3"]
                 [enlive "1.1.6"]
                 [hawk "0.2.11"]
                 [hiccup "1.0.5"]
                 [io.aviso/pretty "0.1.37"]
                 [me.raynes/fs "1.4.6"]
                 [pandect "0.6.1"]
                 [selmer "1.12.18"]
                 [prismatic/schema "1.1.12"]]
  :deploy-repositories [["snapshots" :clojars]
                        ["releases" :clojars]]
  :source-paths ["src"]
  :resource-paths ["resources"]
  :profiles {:dev {:source-paths ["test"]
                   :resource-paths ["test-resources"]
                   :dependencies []
                   :leiningen/reply
                   {:dependencies [[org.slf4j/jcl-over-slf4j "1.8.0-beta0"]
                                   [dda/dummy "0.1.0-SNAPSHOT"]]
                    :exclusions [commons-logging]}}
             :test {:source-paths ["test"]
                    :resource-paths ["test-resources"]
                    :dependencies [[dda/dummy "0.1.0-SNAPSHOT"]]}}
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["uberjar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
