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
      (prn (str "problem detected in thread: [" (.getName thread) "]") throwable))))

(defn default-rejected-fn [runnable _]
  (throw (RejectedExecutionException.
           (str "rejected execution: " runnable))))

(defn- rejected-handler [f]
   (reify RejectedExecutionHandler
     (rejectedExecution [_ runnable executor]
       (f runnable executor))))

(defn thread-factory [name]
  (let [counter (AtomicInteger.)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (let [t (Thread. runnable)]
          (doto t
            (.setName (str name "-" (.incrementAndGet counter)))
            (.setDaemon true)
            (.setUncaughtExceptionHandler (uncaught-exception-handler))))))))

(defn pool
  ([]
   (pool {}))
  ([{:keys [threads
            max-threads
            name
            limit
            thread-factory
            keep-alive-ms
            allow-core-thread-timeout
            rejected-fn
            queue]
             :or {threads (number-of-threads)
                  name "lasync-thread"
                  keep-alive-ms 60000
                  allow-core-thread-timeout false
                  limit 1024
                  rejected-fn default-rejected-fn
                  thread-factory (thread-factory name)}}]

  (let [queue (or queue (ArrayLimitedQueue. limit))
        max-threads (or max-threads threads)]
    (when (> threads max-threads)
      (throw (RuntimeException. (str "core thread number (" threads
                                     ") can't exceed max-threads (" max-threads ")"))))
    (doto (ThreadPoolExecutor. threads
                         max-threads
                         keep-alive-ms TimeUnit/MILLISECONDS
                         queue
                         thread-factory
                         (rejected-handler rejected-fn))
          (.allowCoreThreadTimeOut allow-core-thread-timeout)))))

(defn stats [pool]
  (-> pool
      bean
      (dissoc :rejectedExecutionHandler
              :threadFactory
              :queue)
      (assoc :queueCurrentSize (-> pool .getQueue .size)
             :keepAliveTimeMs (.getKeepAliveTime pool TimeUnit/MILLISECONDS)
             :allowsCoreThreadTimeOut (.allowsCoreThreadTimeOut pool))))

(defn submit [pool f]
  (let [f (if (fn? f)          ;; if f is not fn, wrap it in one
            f
            (fn [] f))
        task (reify Callable
               (call [_]
                 (f)))]
    (.submit pool task)))

(defn execute [pool f]
  (.execute pool f))

(defn shutdown [pool]
  (.shutdownNow pool))

(defn fork-cat [pool fs]
  "a.k.a. 🔱 🐱 "
  (->> fs
       (mapv #(submit pool %))
       (mapv #(.get %))))
