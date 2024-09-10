(ns ez-wire.wiring
  (:require [clojure.zip :as zip]
            [ez-wire.zipper :refer [zipper]]))

(defn marker [node]
  (when (and (keyword? node)
             (or (= "$" (subs (name node) 0 1))
                 (and (namespace node)
                      (= "$" (subs (namespace node) 0 1)))))
    node))

(defn unwrapper [loc markers]
  (let [rest-of-location (zip/rights loc)]
    (-> loc
        (zip/up)
        (zip/replace rest-of-location))))

(defn marker-component [comp & args]
  (fn [loc _markers]
    (let [new-loc (zip/replace loc comp)]
      (reduce (fn [out arg]
                (zip/insert-right out arg))
              new-loc (reverse args)))))

(defn wire [markers loc]
  (let [node (zip/node loc)
        mk (marker node)]
    (if (contains? markers mk)
      (let [value (get markers mk)]
        (cond
          (fn? value) (value loc markers)
          :else (zip/replace loc value)))
      loc)))

(defn wiring [frame markers]
  (try
    (loop [loc (zipper frame)]
      (let [next-loc (zip/next loc)]
        (if (zip/end? next-loc)
          (zip/root loc)
          (recur (wire markers next-loc)))))
    (catch js/Exception e
      (.log js/console e))))
