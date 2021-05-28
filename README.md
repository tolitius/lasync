## limited async

an executor service (a.k.a. smart pool of threads) that is backed by an [ArrayLimitedQueue](src/java/lasync/limitq/ArrayLimitedQueue.java) or a [LinkedLimitedQueue](src/java/lasync/limitq/LinkedLimitedQueue.java).

[![<! release](https://img.shields.io/badge/dynamic/json.svg?label=release&url=https%3A%2F%2Fclojars.org%2Ftolitius%2Flasync%2Flatest-version.json&query=version&colorB=blue)](https://github.com/tolitius/lasync/releases)
[![<! clojars](https://img.shields.io/clojars/v/tolitius/lasync.svg)](https://clojars.org/tolitius/lasync)

- [why](#why)
- [how To](#how-to)
  - [number of threads](#number-of-threads)
  - [queue size](#queue-size)
- [show me](#show-me)
- [tweaking other knobs](#tweaking-other-knobs)
    - [queue implementation](#queue-implementation)
    - [thread factory](#thread-factory)
    - [rejected execution handler](#rejected-execution-handler)
    - [unDefault it](#undefault-it)
  - [shut it down](#shut-it-down)
- [license](#license)

## why

the purpose of this library is to be able to block on "`.submit`" / "`.execute`" whenever the q task limit is reached. Here is why..

if a regular [BlockingQueue](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/BlockingQueue.html) is used,
a ThreadPoolExecutor calls queue's "[offer](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/BlockingQueue.html#offer\(E\))"
method which does not block: inserts a task and returns true, or returns false in case a queue is "capacity-restricted" and its capacity was reached.

while this behavior is useful, there are cases where we do need to _block_ and wait until a ThreadPoolExecutor has
a thread available to work on the task.

depending on a use case this back pressure can be very useful. One reason could be an off heap storage that is being read and processed
by a ThreadPoolExecutor: e.g. there is no need, and sometimes completely undesired, to use JVM heap for something that is already available off heap.
Another good use is described in ["Creating a NotifyingBlockingThreadPoolExecutor"](https://web.archive.org/web/20130111220826/https://today.java.net/pub/a/today/2008/10/23/creating-a-notifying-blocking-thread-pool-executor.html).

## how To

to create a pool with limited number of threads and a backing q limit:

```clojure
(ns sample.project
  (:require [lasync.core :as lasync]))

(def pool (lasync/pool))
```

that is pretty much it. The pool is a regular [ExecutorService](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html) that can have tasks submitted to it:

```clojure
(.submit pool #(+ 41 1))
```

there is also a `submit` function that wraps this call and returns a future:

```clojure
show=> (lasync/submit pool #(+ 41 1))
#object[java.util.concurrent.FutureTask 0x6d1ce6d3 "java.util.concurrent.FutureTask@6d1ce6d3"]
```

as well as an `execute` function that _does not return a future_, hence exeptions will be caught and reported by the default exception handler.

### number of threads

by default lasync will create `available cores * 2 + 42` number of threads:

```clojure
(defn- number-of-threads []
  (+ (* 2 available-cores) 42))
```

but the number can be changed by:

```clojure
user=> (def pool (lasync/pool {:threads 42}))
#'user/pool
```

### queue size

the default queue that is backing lasync's pool is `ArrayLimitedQueue` with a default capacity of `1024` items. But all defaults are there to customize.
A queue size is what limits the pool _enabling the back pressure_. Use `:limit` to tune that knob:

```clojure
(def pool (lasync/pool {:limit 65535}))
```

## show me

to see lasync in action:

```clojure
lein repl
```

```clojure
user=> (require '[show :refer [rock-on]])
```

```clojure
user=> (rock-on 69)  ;; Woodstock'69
```

```
INFO: pool q-size: 4, submitted: 1
INFO: pool q-size: 4, submitted: 3
INFO: pool q-size: 4, submitted: 2
INFO: pool q-size: 4, submitted: 0
INFO: pool q-size: 4, submitted: 4
INFO: pool q-size: 4, submitted: 5
INFO: pool q-size: 4, submitted: 6
INFO: pool q-size: 4, submitted: 7
...
...
INFO: pool q-size: 4, submitted: 62
INFO: pool q-size: 3, submitted: 60
INFO: pool q-size: 4, submitted: 63
INFO: pool q-size: 3, submitted: 65
INFO: pool q-size: 3, submitted: 64
INFO: pool q-size: 2, submitted: 66
INFO: pool q-size: 1, submitted: 67
INFO: pool q-size: 0, submitted: 68
```

here lasync show was rocking on 4 core box (which it picked up on), so regardless of how many tasks are being pushed to it,
the queue max size always stays at 4, and lasync creates that back pressure in case the task q limit is reached.
In fact the "blocking" can be seen in action, as each task is sleeping for a second,
so the whole thing can be visually seen being processed by 4, pause, next 4, pause, etc..

here is [the code](dev/show.clj) behind the show

## tweaking other knobs

#### queue implementation

while `ArrayLimitedQueue` fits most of the use cases, a custom, or a different queue can be configured via `:queue`:

```clojure
(def pool (lasync/pool {:queue (LinkedLimitedQueue. 128)}))
```

#### thread factory

by default lasync's thread factory tries to have reasonable defaults but if you want to make your it's simply a matter
of reify'ing an interface.

```clojure
(def tpool (reify
             ThreadFactory
             (newThread [_ runnable] ...)))

(def pool (lasync/pool {:threads 10 :thread-factory tpool}))
```

#### rejected execution handler

lasync takes an optional `rejected-fn` that will be called on every `RejectedExecutionException`. The default function is:

```clojure
(defn default-rejected-fn [runnable _]
  (throw (RejectedExecutionException.
           (str "rejected execution: " runnable))))
```

but it can be replaced with a custom one (the second param is an `executor`, it is ignored in this case):

```clojure
(defn log-rejected [runnable _]
  (error runnable "was rejected"))

(def pool (lasync/pool {:threads 10 :rejected-fn log-rejected}))
```

#### unDefault it

```clojure
(def tpool (reify ThreadFactory
                 (newThread [_ runnable] ...)))

(defn log-rejected [runnable _]
  (error runnable "was rejected"))

(def lp (lasync/pool {:threads 42
                      :thread-factory tpool
                      :limit 101010101
                      :rejected-fn log-rejected}))
```

### shut it down

when you done with a pool it is a good idea to shut it down:

```clojure
(lasync/shutdown pool)
```

## license

copyright © 2021 tolitius

distributed under the Eclipse Public License, the same as Clojure.
