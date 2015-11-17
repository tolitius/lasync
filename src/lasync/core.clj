(ns lasync.core
  (:require [clojure.tools.logging :refer [info warn debug error]])
  (:import [lasync.limitq ArrayLimitedQueue]
           [java.util.concurrent ThreadPoolExecutor TimeUnit RejectedExecutionHandler ThreadFactory]
           [java.util.concurrent.atomic AtomicInteger]))

(defonce available-cores 
  (.. Runtime getRuntime availableProcessors))

(defn- thread-exception-handler []
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread throwable]
      (error "Problem occured in " (.getName thread) throwable))))

(defn- rejected-exec-handler
  ([]
   (reify RejectedExecutionHandler
     (rejectedExecution [_ runnable executor]
       (error "Rejected exec" runnable))))
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

(defn limit-pool [& {:keys [nthreads queue thread-factory rejected-handler]
                     :or {nthreads available-cores rejected-handler (rejected-exec-handler)
                          queue (ArrayLimitedQueue. 128)
                          thread-factory (thread-factory "lasync-thread")}}]
  (ThreadPoolExecutor. nthreads nthreads 1 TimeUnit/MILLISECONDS
                       queue thread-factory rejected-handler))

