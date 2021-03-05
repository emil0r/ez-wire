(ns ez-wire.form.template
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn row [{:keys [wiring]} _]
  wiring)

(defn- adapt-wiring [{:keys [template] :as params} form]
  (assoc params :wiring
         (reduce (fn [out [_ {:keys [name]}]]
                   (assoc out name template))
                 {} (:fields form))))

(defn as-template [{:keys [template/element]
                    :or {element :div}
                    :as params}
                   {:keys [id form-key] :as form} content]
  (let [{:keys [template]} params]
    (r/create-class
     {:display-name "as-template"

      :component-will-unmount
      (fn [this]
        (when (util/select-option :form/automatic-cleanup? form params)
          (rf/dispatch [:ez-wire.form/cleanup id])))

      :reagent-render
      (fn [params form content]
        (let [{:keys [style
                      class]
               :or {style {}
                    class ""}} params]
          [element {:key (util/slug "form-template" @form-key)
                    :style style
                    :class class}
           [common/get-body row (adapt-wiring params form) form]
           (if content
             [content])]))})))


