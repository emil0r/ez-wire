(ns ez-wire.form.common
  (:require [clojure.spec.alpha :as spec]
            [ez-wire.protocols :refer [t]]
            [ez-wire.wiring :as wiring]
            [reagent.core :as reagent]))


;; allow override of wiring per rendering type (table, list, etc)
(spec/def ::wiring (spec/map-of keyword? any?))
;; allow override of everything
(spec/def ::template vector?)
(spec/def ::form map?)
(spec/def ::content fn?)


(defn render-error-element [{:keys [error-element name error-class]
                             :or {error-class "error"}
                             :as field} form-map]
  (when-not (#{:dispatch} error-element)
    (if error-element
      [error-element {:model (get-in form-map [:errors name])
                      :class [error-class]}])))


(defn render-field [{:keys [field-fn name] :as field} form-map]
  [field-fn (-> field
                (assoc :model (reagent/cursor (:data form-map) [name]))
                (dissoc :wiring :template :label))])


(defn render-text [{:keys [field-fn text css] :as field} form-map]
  (if text
    [:div {:class (get css :text "text")} (t text)]))

(defn render-help [{:keys [field-fn help css] :as field} form-map]
  (if help
    [:div {:class (get css :help "help")} (t help)]))

(defn render-label [{:keys [css id label name] :as field} form-map]
  [:label {:for id :class (get css :label "label")} (t (or label name))])


(defn assoc-wiring [{:keys [name] :as field} params]
  (let [params-wiring (:wiring params)]
    (assoc field :wiring
           (cond (contains? params-wiring name)
                 (get params-wiring name)

                 :else
                 (:wiring field)))))

(defn get-body [row-fn params form-map]
  (doall
   (map (fn [field]
          (let [field (assoc-wiring field params)
                ;; fetch the row
                row (row-fn field form-map)]
            ;; if we have wiring or label-wiring for the field we replace it using wiring

            (cond (:wiring field)
                  (wiring/wiring row {:$wrapper wiring/unwrapper
                                      :$key     {:key (str "ez-wire-form-" (:id field))}
                                      :$label   (render-label field form-map)
                                      :$field   (render-field field form-map)
                                      :$errors  (render-error-element field form-map)
                                      :$text    (render-text field form-map)
                                      :$help    (render-help field form-map)})


                  ;; otherwise we're good to go with using the default row
                  :else
                  row)))
        (map #(get-in form-map [:fields %]) (:field-ks form-map)))))
