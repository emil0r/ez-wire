(ns ez-wire.form.wire
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.wiring :as wiring]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn- ->kw [name k]
  (keyword (str "$" (subs (str name) 1) "." (clojure.core/name k))))

(defn- assemble-body [{:keys [wiring]} {:keys [fields] :as form} content]
  (let [default-map (if content
                      {:$content [content]}
                      {})]
    (wiring/wiring wiring (reduce (fn [out [_ {:keys [name] :as field}]]
                                   (merge out
                                          {(->kw name :wrapper) wiring/unwrapper
                                           (->kw name :key)     {:key (str "ui-wire-form-wire" (:id field))}
                                           (->kw name :label)   (common/render-label field form)
                                           (->kw name :field)   (common/render-field field form)
                                           (->kw name :errors)  (common/render-error-element field form)
                                           (->kw name :text)    (common/render-text field form)
                                           (->kw name :help)    (common/render-help field form)}))
                                 default-map fields))))

(defn as-wire [{:keys [wiring/element]
                :or {element :div}
                :as params}
               {:keys [id form-key] :as form} & [content]]
  (let [body (assemble-body params form content)]
    (r/create-class
     {:display-name "as-wire"

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
          [element {:key (util/slug "form-wire" @form-key)
                    :style style
                    :class class}
           body]))})))
