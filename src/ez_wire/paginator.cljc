(ns ez-wire.paginator)

(defmulti paginate
  "Paginate the incoming collection/length"
  (fn [coll? _ _] (sequential? coll?)))
(defmethod paginate true [coll page-size page]
  (paginate (count coll) page-size page))
(defmethod paginate :default [length page-size page]
  (let [pages (+ (int (/ length page-size))
                 (if (zero? (mod length page-size))
                   0
                   1))
        page (if (and (string? page)
                      (not= page ""))
               #?(:clj  (Integer/parseInt page)
                  :cljs (js/parseInt page))
               page)
        page (cond
              (nil? page) 1
              (or (neg? page) (zero? page)) 1
              (> page pages) pages
              :else page)
        next (+ page 1)
        prev (- page 1)]
    (let [prev (if (or (neg? prev) (zero? prev)) nil prev)]
      {:pages pages
       :page page
       :length length
       :page-size page-size
       :next-seq (range (inc page) (inc pages))
       :prev-seq (reverse (range 1 (if (nil? prev) 1
                                       (inc prev))))
       :next (if (> next pages) nil next)
       :prev prev})))
