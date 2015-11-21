(ns lasync.core
  (:require [clojure.tools.logging :refer [info warn debug error]])
  (:import [lasync.limitq ArrayLimitedQueue]
           [java.util.concurrent ThreadPoolExecutor TimeUnit RejectedExecutionHandler ThreadFactory]
           [java.util.concurrent.atomic AtomicInteger]))

(defonce available-cores 
  (.. Runtime getRuntime availableProcessors))

(defn- default-num-threads []
  (* 2 available-cores))

(defn- thread-exception-handler []
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread throwable]
      (error (str "problem detected in thread: [" (.getName thread) "]") throwable))))

(defn- rejected-exec-handler
  ([]
   (reify RejectedExecutionHandler
     (rejectedExecution [_ runnable executor]
       (error "rejected execution: " runnable))))
  ([f]
   (reify RejectedExecutionHandler
     (rejectedExecution [_ runnable executor]
       (f runnable executor)))))

(defn- thread-factory [name]
  (let [counter (AtomicInteger.)
        ueh (thread-exception-handler)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (let [t (Thread. runnable)]
          (doto t
            (.setName (str name "-" (.incrementAndGet counter)))
            (.setDaemon true)
            (.setUncaughtExceptionHandler ueh)))))))

(defn limit-pool [& {:keys [threads limit thread-factory rejected-handler queue]
                     :or {threads (default-num-threads)
                          limit 1024
                          rejected-handler (rejected-exec-handler)
                          thread-factory (thread-factory "lasync-thread")}}]

  (let [queue (or queue (ArrayLimitedQueue. limit))]
    (ThreadPoolExecutor. threads threads 1 TimeUnit/MILLISECONDS
                         queue thread-factory rejected-handler)))
