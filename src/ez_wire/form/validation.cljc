(ns ez-wire.form.validation
  "Default implementation for handling validation and error messages"
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as spec]
            [ez-wire.form.protocols :as protocols]))

(defonce errors (atom {}))

(defmacro defvalidation
  ([spec-k t-fn-or-keyword]
   `(swap! errors assoc ~spec-k ~t-fn-or-keyword))
  ([spec-k spec-v t-fn-or-keyword]
   `(do (spec/def ~spec-k ~spec-v)
        (swap! errors assoc ~spec-k ~t-fn-or-keyword))))

(defrecord MultiValidation [ks function]
  protocols/IValidate
  (valid? [this value form]
    (let [values (select-keys @(:data form) ks)]
      (function {:values values :form form :validation this})))
  (get-error-message [this value form]
    (get @errors this nil))
  (get-affected-fields [this]
    ks))

(defmacro defmultivalidation
  ([name ks function t-fn-or-keyword]
   `(do (assert (set? ~ks) "ks need to be a set")
        (def ~name (map->MultiValidation {:ks ~ks :function ~function}))
        (swap! errors assoc ~name ~t-fn-or-keyword))))

(extend-protocol protocols/IValidate
  nil
  (valid? [validation value form]
    false)
  (get-error-message [validation value form]
    nil)
  (get-affected-fields [validation]
    #{})
  #?@(:cljs [cljs.core/Keyword
             (valid? [validation value form]
                     (spec/valid? validation value))
             (get-error-message [validation value form]
                                (get @errors validation nil))
             (get-affected-fields [validation]
                                 #{})
             cljs.core/PersistentVector
             (valid? [validation value form]
                     (every? #(protocols/valid? % value form) validation))
             (get-error-message [validation value form]
                                (map #(get @errors % nil) validation))
             (get-affected-fields [validation]
                                  (apply set/union (map #(protocols/get-affected-fields %) validation)))]))

(defrecord ExternalError [id message valid?]
  protocols/IValidate
  (valid? [this value form]
    (:valid? this))
  (get-error-message [this value form]
    message)
  (get-affected-fields [this]
    #{}))

(defn external-error [id message valid?]
  (assert (boolean? valid?) "pass? needs to be a boolean")
  (map->ExternalError {:id id
                       :message message
                       :valid? valid?}))
