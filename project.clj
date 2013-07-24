(defproject lasync "0.1.1-SNAPSHOT"
  :description "executor service that blocks on .submit after the task queue limit is reached"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :source-paths ["src" "src/lasync"]
  :java-source-paths ["src/java"]          

  :dependencies [[org.clojure/tools.logging "0.2.6"]
                 [org.clojure/clojure "1.5.1"]])
