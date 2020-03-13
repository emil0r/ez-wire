(ns ez-wire.form.elements
  (:require [ez-wire.protocols :refer [t]]))

(defn error-element [{:keys [model class]}]
  [:<>
   (for [error @model]
     [:div {:class class :key error} (t error)])])
