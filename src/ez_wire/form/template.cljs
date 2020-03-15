(ns ez-wire.form.template
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]))

(defn row [{:keys [wiring]} _]
  wiring)

(defn- adapt-wiring [{:keys [template] :as params} form-map]
  (assoc params :wiring
         (reduce (fn [out [_ {:keys [name]}]]
                   (assoc out name template))
                 {} (:fields form-map))))

(defn as-template [params form-map content]
  (let [{:keys [id template]
         :or   {id (util/gen-id)}} params
        body (common/get-body row (adapt-wiring params form-map) form-map)
        re-render? (helpers/re-render? form-map)]
    (fn [params form-map content]
      (let [{:keys [style
                    class]
             :or {style {}
                  class ""}} params
            body (if re-render? (common/get-body row (adapt-wiring params form-map) form-map) body)]
        [:div {:key (util/slug "form-template" id)
               :style style
               :class class}
         body
         (if content
           [content])]))))


