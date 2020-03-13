(ns ez-wire.zipper
  (:require [clojure.zip :as zip]))


(defmulti branch? type)
(defmethod branch? :default [_] false)
(defmethod branch? cljs.core/PersistentVector [v] true)
(defmethod branch? cljs.core/PersistentArrayMap [m] true)
(defmethod branch? cljs.core/List [l] true)
(defmethod branch? cljs.core/IndexedSeq [s] true)
(defmethod branch? cljs.core/LazySeq [s] true)
(defmethod branch? cljs.core/Cons [s] true)


(defmulti seq-children type)
(defmethod seq-children cljs.core/PersistentVector [v] v)
(defmethod seq-children cljs.core/PersistentArrayMap [m] (mapv identity m))
(defmethod seq-children cljs.core/List [l] l)
(defmethod seq-children cljs.core/IndexedSeq [s] s)
(defmethod seq-children cljs.core/LazySeq [s] s)
(defmethod seq-children cljs.core/Cons [s] s)

(defmulti make-node (fn [node children] (type node)))
(defmethod make-node cljs.core/PersistentVector [v children] (vec children))
(defmethod make-node cljs.core/PersistentArrayMap [m children] (into {} children))
(defmethod make-node cljs.core/List [_ children] children)
(defmethod make-node cljs.core/IndexedSeq [node children] (apply list children))
(defmethod make-node cljs.core/LazySeq [node children] (apply list children))
(defmethod make-node cljs.core/Cons [node children] (apply list children))

(prefer-method make-node cljs.core/List cljs.core/IndexedSeq)
(prefer-method make-node cljs.core/List cljs.core/LazySeq)
(prefer-method branch? cljs.core/List cljs.core/IndexedSeq)
(prefer-method branch? cljs.core/List cljs.core/LazySeq)
(prefer-method seq-children cljs.core/List cljs.core/IndexedSeq)
(prefer-method seq-children cljs.core/List cljs.core/LazySeq)

(defn zipper [node]
  (zip/zipper branch? seq-children make-node node))
