(ns ez-wire.form.protocols)

(defprotocol IValidate
  :extend-via-metadata true
  (valid? [validation value form])
  (get-error-message [validation value form]))
