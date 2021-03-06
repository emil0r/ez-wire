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


(defn render-error-element [{:keys [error-element name css]
                             :as field} form]
  (when-not (#{:dispatch} error-element)
    (if error-element
      [error-element {:model (get-in form [:errors name])
                      :class (get css :error "error")}])))



(defn- get-branching-args [branch-data]
  (reduce merge (vals branch-data)))

(defn render-field [{:keys [field-fn name] :as field} {:keys [branching] :as form}]
  [field-fn (-> field
                (merge (get (get-branching-args @branching) name))
                (assoc :model (reagent/cursor (:data form) [name]))
                (dissoc :wiring :template :label))])


(defn render-text [{:keys [field-fn text css] :as field} form]
  (if text
    [:div {:class (get css :text "text")} (t text)]))

(defn render-help [{:keys [field-fn help css] :as field} form]
  (if help
    [:div {:class (get css :help "help")} (t help)]))

(defn render-label [{:keys [css id label name] :as field} form]
  (if (false? label)
    nil
    [:label {:for id :class (get css :label "label")} (t (or label name))]))


(defn assoc-wiring [{:keys [name] :as field} params]
  (let [params-wiring (:wiring params)]
    (assoc field :wiring
           (cond (contains? params-wiring name)
                 (get params-wiring name)

                 :else
                 (:wiring field)))))

(defn render-wiring [{:keys [wiring active?] :as field} form]
  (let [rendered (wiring/wiring [:$wrapper
                                 wiring]
                                {:$wrapper wiring/unwrapper
                                 :$key     {:key (str "ez-wire-form-" (:id field))}
                                 :$label   (render-label field form)
                                 :$field   (render-field field form)
                                 :$errors  (render-error-element field form)
                                 :$text    (render-text field form)
                                 :$help    (render-help field form)})]
    (fn [{:keys [active?]} _]
      (when @active?
        (into [:<>] rendered)))))


(defn get-body [row-fn params form]
  (let [fields (map (comp #(assoc-wiring % params)
                          #(get-in form [:fields %])) (:field-ks form))]
    (fn [row-fn params form]
      [:<>
       (for [field fields]
         (if (:wiring field)
           ^{:key (:id field)}
           [render-wiring field form]
           ^{:key (:id field)}
           [row-fn field form]))])))
