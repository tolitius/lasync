(ns lasync.core
  (:import [limitq LimitedQueue]
           [java.util.concurrent ThreadPoolExecutor TimeUnit]))

(defonce available-cores 
  (.. Runtime getRuntime availableProcessors))

(defn- create-thread-pool [qlimit nthreads]
  (ThreadPoolExecutor. nthreads 
                       nthreads 
                       0 
                       TimeUnit/MILLISECONDS
                       (LimitedQueue. qlimit)))

(defn limit-pool [& {:keys [limit nthreads] 
                     :or {limit available-cores
                          nthreads available-cores}}]
  (create-thread-pool limit 
                      nthreads))

