(defproject tolitius/lasync "0.1.15"
  :description "executor service that blocks on .submit after the task queue limit is reached"
  :url "https://github.com/tolitius/lasync"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]
  :java-source-paths ["src/java"]

  :scm {:name "git"
        :url "https://github.com/tolitius/lasync"}

  :profiles {:dev {:source-paths ["dev"]
                   :repl-options {:init-ns show}
                   :dependencies [[ch.qos.logback/logback-classic "1.1.3"]
                                  [org.clojure/tools.logging "0.4.0"]]}}

  :dependencies [[org.clojure/clojure "1.10.1"]])
