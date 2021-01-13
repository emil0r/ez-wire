(ns ez-wire.admin.events
  (:require [re-frame.core :as rf]))


(defn register-module [db [_ {:keys [module/name] :as module}]]
  (let [modules (get db :ez-wire.admin/modules {})]
    (assoc db :ez-wire.admin/modules (assoc modules name module))))

(rf/reg-event-db :ez-wire.admin/register-module register-module)


(defn unregister-module [db [_ module-name]]
  (let [modules (get db :ez-wire.admin/modules [])]
    (assoc db :ez-wire.admin/modules (dissoc modules module-name))))

(rf/reg-event-db :ez-wire.admin/unregister-module unregister-module)
