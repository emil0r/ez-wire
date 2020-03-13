(ns ez-wire.form.macros)

(defmacro defform [-name options fields]
  (let [form-name (name -name)]
    `(defn ~-name
       ([~'data]
        (~-name nil ~'data))
       ([~'opts ~'data]
        (ez-wire.form/form ~fields (assoc ~options :form-name ~form-name) ~'opts ~'data)))))
