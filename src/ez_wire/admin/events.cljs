(ns ez-wire.admin.events
  (:require [re-frame.core :as rf]))


(defn register-module [db [_ {:keys [module/key] :as module}]]
  (let [modules (get db :ez-wire.admin/modules {})]
    (assoc db :ez-wire.admin/modules (assoc modules key module))))
(rf/reg-event-db :ez-wire.admin/register-module register-module)


(defn unregister-module [db [_ module-key]]
  (let [modules (get db :ez-wire.admin/modules [])]
    (assoc db :ez-wire.admin/modules (dissoc modules module-key))))
(rf/reg-event-db :ez-wire.admin/unregister-module unregister-module)


(defn set-module-page [db [_ module-key page]]
  (assoc-in db [:ez-wire.admin/modules module-key :page] page))
(rf/reg-event-db :ez-wire.admin.module/set-page set-module-page)

(defn clear-module-page [db [_ module-key]]
  (update-in db [:ez-wire.admin/modules module-key] dissoc :page))
(rf/reg-event-db :ez-wire.admin.module/clear-page clear-module-page)

(defn clear-pages [db _]
  (reduce (fn [out module-key]
            (update-in out [:ez-wire.admin/modules module-key] dissoc :page))
          db (keys (:ez-wire.admin/modules db))))
(rf/reg-event-db :ez-wire.admin/clear-pages clear-pages)


(defn admin-view [db [_ view]]
  (assoc db :ez-wire.admin/view view))
(rf/reg-event-db :ez-wire.admin/view admin-view )
