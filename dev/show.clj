(ns show
  (:require [lasync.core :as lasync]
            [clojure.tools.logging :as log])
  (:import [lasync.limitq ArrayLimitedQueue LinkedLimitedQueue]))

(defn qsize [pool]
  (.. pool getQueue size))

(defn stats [pool submitted]
  (log/info "pool q-size: " (qsize pool) ", submitted: " submitted)
  (Thread/sleep 1000))

(defn rock-on
  ([ntasks]
   (rock-on ntasks {}))
  ([ntasks {:keys [threads q-size]
            :or {threads 4
                 q-size 4}}]
   (let [pool (lasync/pool :threads threads
                           :queue (ArrayLimitedQueue. q-size))]
     (map (fn [n]
            (lasync/submit pool #(stats pool n)))
          (range ntasks)))))
