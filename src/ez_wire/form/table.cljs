(ns ez-wire.form.table
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))


(defn- table-row [{:keys [active?] :as field} {{label? :label?} :options :as form}]
  (when @active?
    (if (false? label?)
      [:tr {:key (str "tr-" (:id field))}
       [:td
        (common/render-field field form)
        (common/render-error-element field form)
        (common/render-text field form)
        (common/render-help field form)]]
      [:tr {:key (str "tr-" (:id field))}
       [:td
        (common/render-label field form)]
       [:td
        (common/render-field (dissoc field :label) form)
        (common/render-error-element field form)
        (common/render-text field form)
        (common/render-help field form)]])))

(defn as-table [params {:keys [id form-key] :as form} & [content]]
  (r/create-class
   {:display-name "as-table"

    :component-will-unmount
    (fn [this]
      (when (util/select-option :form/automatic-cleanup? form params)
        (rf/dispatch [:ez-wire.form/cleanup id])))

    :reagent-render
    (fn [params form & [content]]
      (let [{:keys [style class]
             :or {style {}
                  class ""}} params]
        [:table {:key (util/slug "form-table" (str @form-key))
                 :style style
                 :class class}
         [:tbody
          [common/get-body table-row params form]
          (if content
            [content])]]))}))
