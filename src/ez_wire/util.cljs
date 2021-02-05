(ns ez-wire.util
  (:require [clojure.string :as str]
            [reagent.ratom]
            [reagent.impl.template]))

(defn reagent-native-wrapper?
  "Check if x has been wrapped by reagent"
  [x]
  (= (type x) reagent.impl.template/NativeWrapper))

(def ^:private +slug-tr-map+
  (zipmap "ąàáäâãåæăćčĉęèéëêĝĥìíïîĵłľńňòóöőôõðøśșšŝťțŭùúüűûñÿýçżźž"
          "aaaaaaaaaccceeeeeghiiiijllnnoooooooossssttuuuuuunyyczzz"))

(defn lower
  "Converts string to all lower-case.
  This function works in strictly locale independent way,
  if you want a localized version, just use `locale-lower`"
  [s]
  (when (string? s)
    (.toLowerCase s)))

(defn slug
  "Transform text into a URL slug"
  [& s]
  (some-> (lower (str/join " " (flatten s)))
          (str/escape +slug-tr-map+)
          (str/replace #"[^\w\s\d]+" "")
          (str/replace #"\s+" "-")))

(defn deref? [x]
  (condp = (type x)
    reagent.ratom/RAtom true
    reagent.ratom/RCursor true
    reagent.ratom/Reaction true
    false))

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
