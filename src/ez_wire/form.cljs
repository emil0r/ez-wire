(ns ez-wire.form
  (:require [clojure.set :as set]
            [ez-wire.util :as util]
            [ez-wire.form.elements]
            [ez-wire.form.helpers]
            [ez-wire.form.list :as form.list]
            [ez-wire.form.paragraph :as form.paragraph]
            [ez-wire.form.protocols :as form.protocols :refer [get-affected-fields
                                                               get-error-message
                                                               valid?]]
            [ez-wire.form.table :as form.table]
            [ez-wire.form.template :as form.template]
            [ez-wire.form.validation :as form.validation]
            [ez-wire.form.wire :as form.wire]
            [ez-wire.form.wizard :as form.wizard]
            [reagent.core :refer [atom] :as r]
            [re-frame.core :as rf]))


(rf/reg-sub      ::error       (fn [db [_ id field-name]]
                                 (get-in db [::error id field-name] [])))
(rf/reg-sub      ::on-valid    (fn [db [_ id]]
                                 (get-in db [::on-valid id] ::invalid)))
(rf/reg-sub      ::form.wizard/current-step (fn [db [_ id]]
                                              (get-in db [::wizard id :current-step])))

(rf/reg-event-db ::cleanup     (fn [db [_ id]]
                                 (let [{::keys [error on-valid wizard]} db]
                                   (-> db
                                       (assoc ::error (dissoc error id))
                                       (assoc ::on-valid (dissoc on-valid id))
                                       (assoc ::wizard (dissoc wizard id))))))
(rf/reg-event-db ::cleanup-all (fn [db _]
                                 (dissoc db
                                         ::error
                                         ::on-valid
                                         ::wizard)))
(rf/reg-event-db ::error       (fn [db [_ id field-name errors]]
                                 (assoc-in db [::error id field-name] errors)))
(rf/reg-event-db ::on-valid    (fn [db [_ id new-state]]
                                 (assoc-in db [::on-valid id] new-state)))
(rf/reg-event-db ::form.wizard/current-step (fn [db [_ id step]]
                                              (assoc-in db [::wizard id :current-step] step)))


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
                    #(get-error-message % value form)))
         (remove nil?))))

(defn- should-update-error?
  [old-state k v]
  ;; we skip pairs of nils assuming that this is a state of initialization
  (and (not (and (nil? (get old-state k))
                 (nil? v)))
       (not= v (get old-state k))))

(defn- get-validation-errors [form new-state old-state]
  (fn [out [k v]]
    (let [{:keys [validation] :as field} (get-in form [:fields k])]
      ;; update? is used to determine if we should do updates to errors
      ;; we still need all the errors for on-valid to properly fire
      (let [affected-fields (get-affected-fields validation)
            update? (if (empty? affected-fields)
                      (should-update-error? old-state k v)
                      (some #(should-update-error? old-state % (get new-state %))
                            (set/union affected-fields #{k})))]
        (if (valid? validation v form)
          ;; if the validation is valid we change it to hold zero errors
          (conj out [k update? []])
          ;; if the validation is invalid we give an explanation to
          ;; what's wrong
          (conj out [k update? (get-error-messages field v form)]))))))

(defn- get-external-errors [form field-errors]
  (reduce (fn [out [k update? errors]]
            (if-let [external-errors (get-in @(:extra form) [k :field-errors])]
              (let [trimmed-external-errors (remove #(valid? % nil nil) external-errors)]
                ;; run a check if we need to update the external errors
                ;; because some of them are being removed
                (if-not (= (count external-errors)
                           (count trimmed-external-errors))
                  (swap! (:extra form) assoc-in [k :field-errors] trimmed-external-errors))
                (conj out [k update? (->> trimmed-external-errors
                                              (map #(get-error-message % nil nil))
                                              (into errors))]))
              (conj out [k update? errors])))
          [] field-errors))

(defn- add-validation-watcher
  "Add validation checks for the RAtom as it changes"
  [form]
  (let [{{on-valid :on-valid} :options} form]
    (add-watch (:data form) (str "form-watcher-" (:id form))
               (fn [_ _ old-state new-state]
                 ;; get all errors for all fields
                 (let [field-errors (->> (reduce (get-validation-errors form new-state old-state) [] new-state)
                                         (get-external-errors form))]
                   ;; update the RAtoms for the error map
                   (doseq [[k update? errors] field-errors]
                     (when update?
                       (rf/dispatch [::error (:id form) k errors])
                       (reset! (get-in form [:errors k]) errors)))
                   ;; if there are no errors then the form is valid and we can fire off the function
                   (let [valid? (every? empty? (map last field-errors))
                         to-send (if valid? new-state ::invalid)]
                     (when (fn? on-valid)
                       (on-valid to-send))
                     (rf/dispatch [::on-valid (:id form) to-send])))))))

(defn- get-default-value [data name field]
  (get data name (:value field)))

(defrecord Form [fields field-ks options id errors data meta])
(defn form [fields form-options override-options data]
  ;; do the conform here as conform can change the structure of the data
  ;; that comes out in order to show how it came to that conclusion (spec/or for example)
  (let [options (merge {:id (util/gen-id)
                        :form/automatic-cleanup? true}
                       form-options
                       override-options)
        map-fields (->> fields
                        (map (fn [{:keys [name id error-element] :as field}]
                               [name (assoc field
                                            :field-fn (get-field-fn field)
                                            :value (get-default-value data name field)
                                            :error-element (or error-element
                                                               ez-wire.form.elements/error-element)
                                            ;; always generate id in the form so we
                                            ;; can reference it later
                                            :id (or id (util/gen-id)))]))
                        (into (array-map)))
        -data (reduce (fn [out [name field]]
                        (assoc out name (get-default-value data name field)))
                      {} map-fields)
        errors (reduce (fn [out [name _]]
                         (assoc out name (atom [])))
                       {} map-fields)
        
        form (map->Form {:fields       map-fields
                         ;; field-ks control which fields are to be rendered for
                         ;; everything form supports with the exception of wiring
                         :field-ks     (mapv :name fields)
                         :options      options
                         :default-data -data
                         :id           (:id options)
                         :extra        (atom {})
                         :form-key     (atom (random-uuid))
                         :errors       errors
                         :data         (atom {})})]
    (add-validation-watcher form)
    ;; run validation once before we send back our form
    (reset! (:data form) -data)
    form))


(def reset-form! ez-wire.form.helpers/reset-form!)
(def cleanup-form! ez-wire.form.helpers/cleanup-form!)
(def as-list (form.wizard/wizard form.list/as-list))
(def as-paragraph (form.wizard/wizard form.paragraph/as-paragraph))
(def as-table (form.wizard/wizard form.table/as-table))
(def as-template (form.wizard/wizard form.template/as-template))
(def as-wire form.wire/as-wire)
