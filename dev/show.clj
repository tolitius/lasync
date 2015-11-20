(ns show
  (:require [lasync.core :refer :all]
            [clojure.tools.logging :refer [info]])
  (:import [lasync.limitq ArrayLimitedQueue LinkedLimitedQueue]))

(defonce pool (limit-pool :queue (ArrayLimitedQueue. 4)))

(defn qsize [pool]
  (.. pool getQueue size))

(defn stats [pool submitted]
  (info "pool q-size: " (qsize pool) ", submitted: " submitted)
  (Thread/sleep 1000))

(defn rock-on [ntasks]
  (map (fn [n]
         (.submit pool #(stats pool n)))
       (range ntasks)))
