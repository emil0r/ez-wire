(ns ez-wire.form.paragraph
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

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
  [params {:keys [id form-key] :as form-map} & [content]]
  (let [body (common/get-body paragraph params form-map)
        re-render? (helpers/re-render? form-map)]
    (r/create-class
     {:display-name "as-paragraph"

      :component-will-unmount
      (fn [this]
        (rf/dispatch [:ez-wire.form/cleanup id]))
      
      :reagent-render
      (fn [params form-map & [content]]
        (let [{:keys [style
                      class]
               :or {style {}
                    class ""}} params
              body (if re-render? (common/get-body paragraph params form-map) body)]
          [:div {:key (util/slug "form-paragraph" @form-key)
                 :style style
                 :class class}
           body
           (if content
             [content])]))})))
