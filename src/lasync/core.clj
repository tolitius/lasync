(ns lasync.core
  (:import [lasync.limitq ArrayLimitedQueue]
           [java.util.concurrent ThreadPoolExecutor TimeUnit ThreadFactory
                                 RejectedExecutionException RejectedExecutionHandler]
           [java.util.concurrent.atomic AtomicInteger]))

(defonce available-cores 
  (.. Runtime getRuntime availableProcessors))

(defn- number-of-threads []
  (+ (* 2 available-cores) 42))

(defn- uncaught-exception-handler []
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread throwable]
      (throw (RuntimeException. (str "problem detected in thread: [" (.getName thread) "]") throwable)))))

(defn default-rejected-fn [runnable _]
  (throw (RejectedExecutionException. 
           (str "rejected execution: " runnable))))

(defn- rejected-handler [f]
   (reify RejectedExecutionHandler
     (rejectedExecution [_ runnable executor]
       (f runnable executor))))

(defn- thread-factory [name]
  (let [counter (AtomicInteger.)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (let [t (Thread. runnable)]
          (doto t
            (.setName (str name "-" (.incrementAndGet counter)))
            (.setDaemon true)
            (.setUncaughtExceptionHandler (uncaught-exception-handler))))))))

(defn pool [& {:keys [threads limit thread-factory rejected-fn queue]
               :or {threads (number-of-threads)
                    limit 1024
                    rejected-fn default-rejected-fn
                    thread-factory (thread-factory "lasync-thread")}}]

  (let [queue (or queue (ArrayLimitedQueue. limit))]
    (ThreadPoolExecutor. threads threads 1 TimeUnit/MILLISECONDS
                         queue thread-factory (rejected-handler rejected-fn))))

(defn submit [pool f]
  (.submit pool f))
