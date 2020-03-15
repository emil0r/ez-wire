(ns ez-wire.form
  (:require [ez-wire.util :as util]
            [ez-wire.form.elements]
            [ez-wire.form.protocols :as form.protocols :refer [valid? get-error-message]]
            [ez-wire.form.list :as form.list]
            [ez-wire.form.table :as form.table]
            [ez-wire.form.validation :as form.validation]
            [reagent.core :refer [atom] :as r]
            [re-frame.core :as rf]))


(defmulti ^:private get-field-fn (fn [field]
                                   (let [{:keys [element adapter]} field]
                                     (cond adapter                                :adapt-fn
                                           (fn? element)                          :fn
                                           (util/reagent-native-wrapper? element) :fn
                                           (keyword? element)                     :keyword
                                           :else                                  nil))))
(defmethod get-field-fn :fn [field]
  (:element field))
(defmethod get-field-fn :adapt-fn [{:keys [adapter] :as field}]
  (adapter field))
(defmethod get-field-fn :default [field]
  (:element field))

(defn- finalize-error-message [context error-message]
  (if (fn? error-message)
    (with-meta (error-message context) {::by-fn? true})
    error-message))

(defn- get-error-messages [{:keys [validation] :as field} value form]
  (let [v (if (sequential? validation) validation [validation])
        context {:field field
                 :value value
                 :form form}]
    (->> v
         (remove #(valid? % value form))
         (map (comp #(finalize-error-message context %)
                    #(get-error-message % value form))))))

(defn- get-validation-errors [form old-state]
  (fn [out [k v]]
    (let [field (get-in form [:fields k])]
      ;; when there is a validation
      ;; AND, there has been a change in value
      (if (:validation field)
        ;; same-value? is used to determine if we should do updates to errors
        ;; we still need all the errors for on-valid to properly fire
        (let [same-value? (= v (get old-state k))]
          (if (valid? (:validation field) v form)
            ;; if the validation is valid we change it to hold zero errors
            (conj out [k same-value? []])
            ;; if the validation is invalid we give an explanation to
            ;; what's wrong
            (conj out [k same-value? (get-error-messages field v form)])))
        ;; no validation defined, give back the out value
        out))))

(defn- add-validation-watcher
  "Add validation checks for the RAtom as it changes"
  [form]
  (let [{{on-valid :on-valid} :options} form]
    (add-watch (:data form) (str "form-watcher-" (:id form))
               (fn [_ _ old-state new-state]
                 ;; get all errors for all fields
                 (let [field-errors (reduce (get-validation-errors form old-state) [] new-state)]
                   ;; update the RAtoms for the error map
                   (doseq [[k same-value? errors] field-errors]
                     (when-not same-value?
                       (rf/dispatch [::error (:id form) k errors])
                       (reset! (get-in form [:errors k]) errors)))
                   ;; if there are no errors then the form is valid and we can fire off the function
                   (let [valid? (every? empty? (map last field-errors))
                         to-send (if valid? new-state ::invalid)]
                     (when (fn? on-valid)
                       (on-valid to-send))
                     (rf/dispatch [::on-valid (:id form) to-send])))))))

(defn- get-default-value [field]
  (or (:value field) (util/deref-or-value (:model field))))

(defrecord Form [fields field-ks options id errors data meta])
(defn form [fields form-options override-options data]
  ;; do the conform here as conform can change the structure of the data
  ;; that comes out in order to show how it came to that conclusion (spec/or for example)
  (let [options (merge {:id (util/gen-id)} form-options override-options)
        map-fields (->> fields
                        (map (fn [{:keys [name id error-element] :as field}]
                               [name (assoc field
                                            :field-fn (get-field-fn field)
                                            :error-element (or error-element
                                                               ez-wire.form.elements/error-element)
                                            ;; always generate id in the form so we
                                            ;; can reference it later
                                            :id (or id (util/gen-id)))]))
                        (into (array-map)))
        errors (reduce (fn [out [name _]]
                         (assoc out name (atom [])))
                       {} map-fields)
        data (atom (reduce (fn [out [name field]]
                             (assoc out name (get-default-value field)))
                           {} map-fields))
        form (map->Form {:fields  map-fields
                             ;; field-ks control which fields are to be rendered for
                             ;; everything form supports with the exception of wiring
                             :field-ks (mapv :name fields)
                             :options options
                             :id      (:id options)
                             :errors  errors
                             :data    data})]
    (add-validation-watcher form)
    form))


(def as-list form.list/as-list)
(def as-table form.table/as-table)
