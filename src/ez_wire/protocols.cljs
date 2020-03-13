(ns ez-wire.protocols)

(defprotocol Ii18n
  :extend-via-metadata true
  (t [k] [k args]))


;; default implementations

(extend-protocol Ii18n
  string
  (t
    ([k] k)
    ([k args] k))
  nil
  (t
    ([k] k)
    ([k args] k)))
