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

(defn- field-updated?
  [old-state k v]
  ;; we skip pairs of nils assuming that this is a state of initialization
  (let [old-v (get old-state k)]
    (and (not (and (nil? old-v)
                   (nil? v)))
         (not= v old-v))))

(defn- get-validation-errors [form new-state old-state]
  (fn [out [k v]]
    (let [{:keys [validation] :as field} (get-in form [:fields k])]
      ;; update? is used to determine if we should do updates to errors
      ;; we still need all the errors for on-valid to properly fire
      (let [affected-fields (get-affected-fields validation)
            update? (if (empty? affected-fields)
                      (field-updated? old-state k v)
                      ;; we need to run two checks here. first that the current field
                      ;; has been updated at least once. if this does not pass
                      ;; we do nothing, because the user has not begun to interact with the
                      ;; field
                      ;; if it passes that we run the check against affected fields,
                      ;; including the current field
                      (and (some? v)
                           (some #(field-updated? old-state % (get new-state %))
                                 (set/union affected-fields #{k}))))]
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

(defn- handle-branching [form field-name value]
  (if (and (some? field-name)
           (get-in form [:options :branch/branching?]))
    (if-let [f (get-in form [:options :branch/branches field-name])]
      (let [result (f form field-name value)
            {:keys [show-fields hide-fields]} result
            show-fields (set (if (= show-fields :all) (:field-ks form) show-fields))
            show-fields (if (:exclude-branching-field? result)
                      show-fields
                      (conj show-fields field-name))
            hide-fields (set (if (= hide-fields :all) (:field-fields form) hide-fields))]
        (doseq [k (set/difference hide-fields show-fields)]
          (reset! (get-in form [:fields k :active?]) false))
        (doseq [k show-fields]
          (reset! (get-in form [:fields k :active?]) true))
        (swap! (:branching form) assoc field-name (:fields result))))))

(defn- get-changed-field [old-state new-state]
  (reduce (fn [out [k value]]
            (if (field-updated? old-state k value)
              (reduced k)
              nil))
          nil new-state))

(defn- add-watcher
  "Add validation and branching checks for the RAtom as it changes"
  [form]
  (let [{{on-valid :on-valid} :options} form
        ;; Watcher is manually run when initiating a form. We
        ;; do not wish to run branching during the initiation however,
        ;; so we add this little variable for initiation to handle
        ;; that problem
        branching-initiated? (clojure.core/atom false)]
    (add-watch (:data form) (:id form)
               (fn [_ _ old-state new-state]
                 (let [changed-field-k (get-changed-field old-state new-state)]
                  ;; get all errors for all fields
                  (let [field-errors (->> (reduce (get-validation-errors form new-state old-state) [] new-state)
                                          (get-external-errors form))]
                    ;; update the RAtoms for the error map
                    (doseq [[k update? errors] field-errors]
                      (when update?
                        (rf/dispatch [::error (:id form) k errors])
                        (reset! (get-in form [:errors k]) errors)))

                    ;; handle branching before validity
                    ;; validity depends on the updated branching to decide
                    ;; which fields are active or not.
                    ;; branching initiation is also handled here.
                    (if @branching-initiated?
                      (when changed-field-k
                        (handle-branching form changed-field-k (get new-state changed-field-k)))
                      (reset! branching-initiated? true))

                    ;; if there are no errors then the form is
                    ;; valid and we can fire off the function
                    (let [active-fields (reduce (fn [out k]
                                                  (if @(:active? (get-in form [:fields k]))
                                                    (conj out k)
                                                    out))
                                                #{} (:field-ks form))
                          valid? (->> field-errors
                                      (filter #(active-fields (first %)))
                                      (map last field-errors)
                                      (every? empty?))
                          to-send (if valid? (select-keys new-state active-fields) ::invalid)]
                      (when (fn? on-valid)
                        (on-valid to-send))
                      (rf/dispatch [::on-valid (:id form) to-send]))))))))

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
                        (map (fn [{:keys [name id error-element active?] :as field}]
                               [name (assoc field
                                            :active? (atom (if (some? active?)
                                                             active?
                                                             true))
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
        field-ks (mapv :name fields)
        form (map->Form {:fields       map-fields
                         ;; field-ks control which fields are to be rendered for
                         ;; everything form supports with the exception of wiring
                         :field-ks     field-ks
                         :options      options
                         :default-data -data
                         :id           (:id options)
                         :extra        (atom {})
                         :form-key     (atom (random-uuid))
                         :branching    (atom {})
                         :wizard       (atom {})
                         :errors       errors
                         :data         (atom {})})]
    (add-watcher form)
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
