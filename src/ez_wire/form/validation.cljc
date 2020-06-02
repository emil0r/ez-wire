(ns ez-wire.form.validation
  "Default implementation for handling validation and error messages"
  (:require [clojure.spec.alpha :as spec]
            [ez-wire.form.protocols :as protocols]))

(defonce errors (atom {}))

(defmacro defvalidation [spec-k spec-v t-fn-or-keyword]
  `(do (spec/def ~spec-k ~spec-v)
       (swap! errors assoc ~spec-k ~t-fn-or-keyword)))

(extend-protocol protocols/IValidate
  nil
  (valid? [validation value form]
    false)
  (get-error-message [validation value form]
    nil)
  #?@(:cljs [cljs.core/Keyword
             (valid? [validation value form]
                     (spec/valid? validation value))
             (get-error-message [validation value form]
                                (get @errors validation nil))
             cljs.core/PersistentVector
             (valid? [validation value form]
                     (every? #(protocols/valid? % value form) validation))
             (get-error-message [validation value form]
                                (map #(get @errors % nil) validation))]))
