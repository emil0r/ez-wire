(ns ez-wire.ui.button
  (:require [ez-wire.ui.styling :refer [get-styling]]
            [reagent.core :as r]))

(defn button [context content]
  (r/with-let [props (-> context
                         (select-keys [:on-click :css :style])
                         (get-styling {:css "ez-wire-button"} ::button))]
    [:button props content]))
