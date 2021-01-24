(ns ez-wire.admin.view.entity
  (:require [ez-wire.admin.helpers :refer [navigate]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn list-entity [module-key entity-key]
  (r/with-let [modules (rf/subscribe [:ez-wire.admin/modules])]
    (let [module (get @modules module-key)
          {:entity/keys [interface] :as entity}
          (get-in module [:module/entities entity-key])]
      (pr-str interface)
      )))
