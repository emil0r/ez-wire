(ns ez-wire.form.protocols)

(defprotocol IValidate
  :extend-via-metadata true
  (valid? [validation value form] "Evaluate if this validation is true given the value")
  (get-error-message [validation value form] "Get any error messages for this validation")
  (get-affected-fields [validation] "Get any fields that might be affected by this validation. This can include the same field as the validation is attached to."))
