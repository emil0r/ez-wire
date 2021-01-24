(ns ez-wire.admin.subs
  (:require [re-frame.core :as rf]))

(defn modules [db _]
  (:ez-wire.admin/modules db))
(rf/reg-sub :ez-wire.admin/modules modules)

(defn module [db [_ module-key]]
  (get-in db [:ez-wire.admin/modules module-key]))
(rf/reg-sub :ez-wire.admin/module module)

;; (defn sub-entity [db [_
;;                       module-name
;;                       entity
;;                       {:keys [?search] :as _params}]]
;;   (let [{:keys [count-per-page
;;                 page
;;                 items
;;                 columns] :as module} (get-in db [:ez-wire.admin/modules module-name])]
;;     {:items (:items module)}))


(defn navigation [db _]
  (get db :ez-wire.admin/navigation {:module :ez-wire.admin/all-modules}))
(rf/reg-sub :ez-wire.admin/navigation navigation)
