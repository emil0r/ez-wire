(ns ez-wire.form.paragraph
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]))

(defn- paragraph [{:keys [wiring] :as field} {{label? :label?} :options :as form-map}]
  (if wiring
    [:$wrapper wiring]
    (if (false? label?)
      [:p {:key (str "li-" (:id field))}
       (common/render-field field form-map)
       (common/render-error-element field form-map)
       (common/render-text field form-map)
       (common/render-help field form-map)]
      [:p {:key (str "li-" (:id field))}
       (common/render-label field form-map)
       (common/render-field (dissoc field :label) form-map)
       (common/render-error-element field form-map)
       (common/render-text field form-map)
       (common/render-help field form-map)])))

(defn as-paragraph
  [params form-map & [content]]
  (let [{:keys [id]
         :or   {id   (util/gen-id)}} params
        body (common/get-body paragraph params form-map)
        re-render? (helpers/re-render? form-map)]
    (fn [params form-map & [content]]
      (let [{:keys [style
                    class]
             :or {style {}
                  class ""}} params
            body (if re-render? (common/get-body paragraph params form-map) body)]
        [:div {:key (util/slug "form-paragraph" id)
               :style style
               :class class}
         body
         (if content
           [content])]))))
