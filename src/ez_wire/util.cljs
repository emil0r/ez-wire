(ns ez-wire.util
  (:require [clojure.string :as str]
            [reagent.ratom]
            [reagent.impl.template]))

(defn reagent-native-wrapper?
  "Check if x has been wrapped by reagent"
  [x]
  (= (type x) reagent.impl.template/NativeWrapper))


(defn deref? [x]
  (satisfies? IDeref x))

(defn deref-or-value [model]
  (if (deref? model) @model model))

(defn gen-id
  "Create a unique-id"
  []
  (str (random-uuid)))

(defn select-k
  "Select a k value from a sequence of maps. Precedence is from first to last"
  [k & maps]
  (reduce (fn [out data]
            (get data k out))
          nil maps))

(defn select-ks
  "Select a value from based on ks from a sequence of maps. Precedence is from first to last"
  [ks & maps]
  (reduce (fn [out data]
            (get-in data ks out))
          nil maps))


(defn select-option [k-or-ks form params]
  (if (vector? k-or-ks)
    (select-ks k-or-ks (:options form) params)
    (select-k k-or-ks (:options form) params)))
