(ns show
  (:require [lasync.core :refer :all]
            [clojure.tools.logging :refer [info]]))

(defonce pool (limit-pool))

(defn qsize [pool]
  (.. pool getQueue size))

(defn stats [pool submitted]
  (info "pool q-size: " (qsize pool) ", submitted: " submitted)
  (Thread/sleep 1000))

(defn rock-on [ntasks]
  (map (fn [n]
         (.submit pool #(stats pool n)))
       (range ntasks)))
