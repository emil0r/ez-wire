(ns ez-wire.form.list
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

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
  [params {:keys [id form-key] :as form-map} & [content]]
  (let [{:keys [list/type]
         :or   {type :ul}} params
        body (common/get-body li params form-map)
        re-render? (helpers/re-render? form-map)]
    (r/create-class
     {:display-name "as-list"

      :component-will-unmount
      (fn [this]
        (when (util/select-option :form/automatic-cleanup? form-map params)
          (rf/dispatch [:ez-wire.form/cleanup id])))
      :reagent-render
      (fn [params form-map & [content]]
        (let [{:keys [style
                      class]
               :or {style {}
                    class ""}} params
              body (if re-render? (common/get-body li params form-map) body)]
          [type {:key (util/slug "form-list" @form-key)
                 :style style
                 :class class}
           body
           (if content
             [content])]))})))
