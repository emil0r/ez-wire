(ns ez-wire.form.template
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn row [{:keys [wiring]} _]
  wiring)

(defn- adapt-wiring [{:keys [template] :as params} form-map]
  (assoc params :wiring
         (reduce (fn [out [_ {:keys [name]}]]
                   (assoc out name template))
                 {} (:fields form-map))))

(defn as-template [{:keys [template/element]
                    :or {element :div}
                    :as params}
                   {:keys [id form-key] :as form-map} content]
  (let [{:keys [template]} params
        body (common/get-body row (adapt-wiring params form-map) form-map)
        re-render? (helpers/re-render? form-map)]
    (r/create-class
     {:display-name "as-template"

      :component-will-unmount
      (fn [this]
        (when (util/select-option :form/automatic-cleanup? form-map params)
          (rf/dispatch [:ez-wire.form/cleanup id])))

      :reagent-render
      (fn [params form-map content]
        (let [{:keys [style
                      class]
               :or {style {}
                    class ""}} params
              body (if re-render? (common/get-body row (adapt-wiring params form-map) form-map) body)]
          [element {:key (util/slug "form-template" @form-key)
                    :style style
                    :class class}
           body
           (if content
             [content])]))})))


