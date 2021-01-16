(ns ez-wire.admin
  (:require [ez-wire.admin.events]
            [ez-wire.admin.subs]
            [ez-wire.admin.view]
            [re-frame.core :as rf]))

(defn register-module [module]
  (rf/dispatch [:ez-wire.admin/register-module module]))

(def admin-index ez-wire.admin.view/index)
