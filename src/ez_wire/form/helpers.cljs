(ns ez-wire.form.helpers
  (:require [ez-wire.form.protocols :as protocols]
            [ez-wire.form.validation :refer [external-error]]
            [ez-wire.util :as util]
            [re-frame.core :as rf]))

(defn valid?
  "Helper function for checking validity of the sub ::on-valid. Accepts a form, subscription or value"
  [v]
  (cond (record? v)
        (not= @(rf/subscribe [:ez-wire.form/on-valid (:id v)]) :ez-wire.form/invalid)

        (util/deref? v)
        (not= @v :ez-wire.form/invalid)

        :else
        (not= v :ez-wire.form/invalid)))

(defn wizard?
  "Is the form a wizard?"
  [form]
  (= :wizard (get-in form [:options :render])))

(defn re-render?
  "Does the form need to be re-rendered (e.g., a wizard?)"
  [form]
  (#{:wizard} (get-in form [:options :render])))


(defn add-external-error [form field-name id message valid?]
  (let [error (->> (get-in @(:extra form) [field-name :field-errors])
                   (filter #(= id (:id %)))
                   first)]
    (when-not error
      (let [new-error (external-error id message valid?)]
        ;; add the new external error message to the extra ratom
        (swap! (get-in form [:extra]) update-in [field-name :field-errors] conj new-error)
        ;; update the current error messages
        (swap! (get-in form [:errors field-name]) conj message)))))

(defn remove-external-error [form field-name id]
  (let [field-errors (get-in @(:extra form) [field-name :field-errors])
        error (->> field-errors
                   (filter #(= id (:id %)))
                   first)
        error-message (protocols/get-error-message error nil nil)
        errors (->> field-errors
                    (remove #(= id (:id %))))
        error-messages (->> @(get-in form [:errors field-name])
                            (remove #(= % error-message)))]
    ;; update the external error messages in the extra ratom
    (swap! (get-in form [:extra]) assoc-in [field-name :field-errors] errors)
    ;; remove the error message from that particular error
    (reset! (get-in form [:errors field-name]) error-messages)))


(defn reset-form!
  "Reset the form to initial state"
  ([form]
   (reset-form! form (:default-data form)))
  ([form data]
   ;; reset all errors to empty arrays
   (doseq [[k v] (:errors form)]
     (reset! v []))
   ;; ;; reset extra with an empty map
   (reset! (:extra form) {})
   
   ;; reset the data. first nil to clean everything,
   ;; then the data to trigger the re-evaluation
   (reset! (:data form) nil)
   (reset! (:data form) (select-keys data (:field-ks form)))
   (reset! (:form-key form) (random-uuid))))


(defn cleanup-form!
  [form]
  (remove-watch (:data form) (:id form))
  (rf/dispatch [:ez-wire.form/cleanup (:id form)]))
