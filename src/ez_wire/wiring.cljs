(ns ez-wire.wiring
  (:require [clojure.string :as str]
            [clojure.zip :as zip]
            [ez-wire.zipper :refer [zipper]]))

(defn marker [node]
  (if (and (keyword? node)
           (str/starts-with? (str node) ":$"))
    node))

(defn unwrapper [loc markers]
  (let [rest-of-location (zip/rights loc)]
    (-> loc
        (zip/up)
        (zip/replace rest-of-location))))

(defn wire [markers loc]
  (let [node (zip/node loc)
        mk (marker node)]
    (if (contains? markers mk)
      (let [value (get markers mk)]
        (if (fn? value)
          (value loc markers)
          (zip/replace loc value)))
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
