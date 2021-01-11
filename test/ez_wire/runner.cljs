(ns ez-wire.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [ez-wire.test-core]))

(doo-tests 'ez-wire.test-core)

