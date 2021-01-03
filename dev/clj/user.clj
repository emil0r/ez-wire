(ns user
 (:require [figwheel-sidecar.repl-api :as ra]))

(defn start-fw []
  (ra/start-figwheel!))

(defn stop-fw []
  (ra/stop-figwheel!))

(defn cljs []
  (ra/cljs-repl))

(comment

  (start-fw)
  (stop-fw)
  (cljs)

  (do (start-fw)
      (cljs))
  )
