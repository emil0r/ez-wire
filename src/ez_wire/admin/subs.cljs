(ns ez-wire.admin.subs
  (:require [re-frame.core :as rf]))

(defn modules [db _]
  (:ez-wire.admin/modules db))

(rf/reg-sub :ez-wire.admin/modules)
