(defproject lasync "0.1.4-SNAPSHOT"
  :description "executor service that blocks on .submit after the task queue limit is reached"
  :url "https://github.com/tolitius/lasync"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src"]
  :java-source-paths ["src/java"]          

  :scm {:name "git"
        :url "https://github.com/tolitius/lasync"}

  :profiles {:dev {:source-paths ["dev"]
                   :repl-options {:init-ns show}}}

  :dependencies [[org.clojure/tools.logging "0.3.1"]
                 [org.clojure/clojure "1.5.1"]])
