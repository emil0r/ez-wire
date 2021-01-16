(ns ez-wire.admin.subs
  (:require [re-frame.core :as rf]))

(defn modules [db _]
  (vals (:ez-wire.admin/modules db)))

(rf/reg-sub :ez-wire.admin/modules modules)

(defn sub-module [db [_
                      module-name
                      {:keys [?search] :as _params}]]
  (let [{:keys [count-per-page
                page
                items
                columns] :as module} (get-in db [:ez-wire.admin/modules module-name])]
    {:items (:items module)}))


(defn admin-view [db _]
  (get db :ez-wire.admin/view {:view :list
                               :module :ez-wire.admin/all-modules}))
(rf/reg-sub :ez-wire.admin/view admin-view)
