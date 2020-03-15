(ns ez-wire.form.list
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]))

(defn- li [{:keys [wiring] :as field} {{label? :label?} :options :as form-map}]
  (if wiring
    [:$wrapper wiring]
    (if (false? label?)
      [:li {:key (str "li-" (:id field))}
       (common/render-field field form-map)
       (common/render-error-element field form-map)
       (common/render-text field form-map)
       (common/render-help field form-map)]
      [:li {:key (str "li-" (:id field))}
       (common/render-label field form-map)
       (common/render-field (dissoc field :label) form-map)
       (common/render-error-element field form-map)
       (common/render-text field form-map)
       (common/render-help field form-map)])))

(defn as-list
  [params form-map & [content]]
  (let [{:keys [id type]
         :or   {id   (util/gen-id)
                type :ul}} params
        body (common/get-body li params form-map)
        re-render? (helpers/re-render? form-map)]
    (fn [params form-map & [content]]
      (let [{:keys [style
                    class]
             :or {style {}
                  class ""}} params
            body (if re-render? (common/get-body li params form-map) body)]
        [type {:key (util/slug "form-list" id)
               :style style
               :class class}
         body
         (if content
           [content])]))))
