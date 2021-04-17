(ns ez-wire.form.elements
  (:require [ez-wire.protocols :refer [t]]))

(defn error-element [{:keys [model class]}]
  [:<>
   (doall
    (for [error @model]
      [:div {:class class :key error}
       (if (true? (:ez-wire.form/by-fn? (meta error)))
         error
         (t error))]))])

(defn button-element [props text]
  [:button props text])
