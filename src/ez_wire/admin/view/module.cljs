(ns ez-wire.admin.view.module
  (:require [ez-wire.admin.helpers :refer [navigate]]
            [ez-wire.protocols :refer [t]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn list-entity [module entity]
  [:div.ez-wire-admin-entity
   [:h2
    {:on-click #(navigate (:module/key module) (:entity/key entity))}
    (t (:entity/name entity))]])

(defn list-module [{:module/keys [key entities display] :as module}]
  [:div.ez-wire-admin-module
   [:h2
    {:on-click #(navigate key)}
    (t (:module/name module))]
   (for [k (:entities/order display)]
     (let [entity (get entities k)]
       ^{:key [::list-module key k]}
       [list-entity module (assoc entity :entity/key k)]))])

(defn list-single-module [module]
  (r/with-let [modules (rf/subscribe [:ez-wire.admin/modules])]
    [:div.ez-wire-admin-modules
     (let [module (get @modules module)]
       [list-module module])]))

(defn list-modules []
  (r/with-let [modules (rf/subscribe [:ez-wire.admin/modules])]
    [:div.ez-wire-admin-modules
     (for [[k module] @modules]
       ^{:key [::module k]}
       [list-module module])]))



