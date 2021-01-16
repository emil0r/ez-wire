(ns ez-wire.admin.view
  (:require [ez-wire.protocols :refer [t]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn list-module [{:keys [] :as module}]
  [:div.ez-wire-admin-module
   [:h2 (t (:module/name module))]])

(defn list-modules []
  (r/with-let [modules (rf/subscribe [:ez-wire.admin/modules])]
    [:div.ez-wire-admin-modules
     (for [{:keys [module/key] :as module} @modules]
       ^{:key [::module key]}
       [list-module module])]))


(defn index []
  (r/with-let [view (rf/subscribe [:ez-wire.admin/view])]
    [:div.ez-wire-admin
     (let [{:keys [view module]} @view]
       (cond (= module :ez-wire.admin/all-modules)
             [list-modules]

             ;; catch all
             :else
             [list-modules]))]))
