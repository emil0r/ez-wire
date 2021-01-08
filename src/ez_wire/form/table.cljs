(ns ez-wire.form.table
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]))


(defn- table-row [{:keys [wiring] :as field} {{label? :label?} :options :as form-map}]
  (if wiring
    [:$wrapper wiring]
    (if (false? label?)
      [:tr {:key (str "tr-" (:id field))}
       [:td
        (common/render-field field form-map)
        (common/render-error-element field form-map)
        (common/render-text field form-map)
        (common/render-help field form-map)]]
      [:tr {:key (str "tr-" (:id field))}
       [:td
        (common/render-label field form-map)]
       [:td
        (common/render-field (dissoc field :label) form-map)
        (common/render-error-element field form-map)
        (common/render-text field form-map)
        (common/render-help field form-map)]])))

(defn as-table
  [params {:keys [form-key] :as form-map} & [content]]
  (let [{:keys [id]
         :or   {id (util/gen-id)}} params
        ;; generate the body of the table
        body (common/get-body table-row params form-map)
        re-render? (helpers/re-render? form-map)]
    (fn [params form-map & [content]]
      (let [{:keys [style class]
             :or {style {}
                  class ""}} params
            body (if re-render? (common/get-body table-row params form-map) body)]
        [:table {:key (util/slug "form-table" (str @form-key))
                 :style style
                 :class class}
         [:tbody body
          (if content
            [content])]]))))
