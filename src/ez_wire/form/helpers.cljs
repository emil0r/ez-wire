(ns ez-wire.form.helpers
  (:require [ez-wire.util :as util]
            [re-frame.core :as rf]))

(defn valid?
  "Helper function for checking validity of the sub ::on-valid. Accepts a form, subscription or value"
  [v]
  (cond (record? v)
        (not= @(rf/subscribe [:ez-wire.form/on-valid (:id v)]) :ui.wire.form/invalid)

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
