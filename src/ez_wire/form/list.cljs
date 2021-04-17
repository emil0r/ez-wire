(ns ez-wire.form.list
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn- li [{:keys [active?] :as field} {{label? :label?} :options :as form}]
  (when @active?
    (if (false? label?)
      [:li {:key (str "li-" (:id field))}
       (common/render-field field form)
       (common/render-error-element field form)
       (common/render-text field form)
       (common/render-help field form)]
      [:li {:key (str "li-" (:id field))}
       (common/render-label field form)
       (common/render-field (dissoc field :label) form)
       (common/render-error-element field form)
       (common/render-text field form)
       (common/render-help field form)])))

(defn as-list
  [params {:keys [id form-key] :as form} & [content]]
  (let [{:keys [list/type]
         :or   {type :ul}} params]
    (r/create-class
     {:display-name "as-list"

      :component-will-unmount
      (fn [this]
        (when (util/select-option :form/automatic-cleanup? form params)
          (rf/dispatch [:ez-wire.form/cleanup id])))
      :reagent-render
      (fn [params form & [content]]
        (let [{:keys [style
                      class]
               :or {style {}
                    class ""}} params]
          [type {:key (util/slug "form-list" @form-key)
                 :style style
                 :class class}
           [common/get-body li params form]
           (if content
             [content])]))})))
