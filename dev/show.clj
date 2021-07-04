(ns show
  (:require [lasync.core :as lasync]
            [clojure.tools.logging :as log])
  (:import [lasync.limitq ArrayLimitedQueue LinkedLimitedQueue]
           [java.util.concurrent ArrayBlockingQueue]))

(defn qsize [pool]
  (.. pool getQueue size))

(defn pstats [pool]
  (select-keys (lasync/stats pool)
               [:poolSize :queueCurrentSize :activeCount]))

(defn stats [pool submitted]
  (Thread/sleep 1000)
  (log/infof  "submitted: %s | %s" submitted (pstats pool)))

(defn rock-on
  ([ntasks]
   (rock-on ntasks {}))
  ([ntasks {:keys [threads
                   queue
                   q-size]
            :or {threads 4
                 q-size 4}
            :as opts}]
   (let [q (or queue (ArrayLimitedQueue. q-size))
         pool (lasync/pool (assoc opts
                                  :threads threads
                                  :queue q))]
     (mapv (fn [n]
             (lasync/submit pool #(stats pool n)))
           (range ntasks))
     pool)))

;; (expire-core-threads 69)
(defn expire-core-threads
  "will release unused _core_ threads
   look at ':poolSize' go down to zero after the keep-alive-ms kicks in"
  [ntasks]
  (let [pool (rock-on ntasks {:keep-alive-ms 10000
                              :allow-core-thread-timeout true})]
    (dotimes [n 10]
      (Thread/sleep 2000)
      (log/info (pstats pool)))))

;; (use-max-threads 69)
(defn use-max-threads
  "in order to use max threads, a traditional queue should be used, otherwise
   thread pool executor won't grow beyond the core thread size"
  [ntasks]
  (let [pool (rock-on ntasks {:max-threads 69
                              :keep-alive-ms 100
                              :queue (ArrayBlockingQueue. 4)})]))
