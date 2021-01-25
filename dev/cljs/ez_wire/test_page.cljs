(ns ez-wire.test-page
  (:require [antd :as ant]
            [ez-wire.test-admin :as admin]
            [ez-wire.test-form :as form]
            [ez-wire.test-ui :as ui]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(enable-console-print!)

(rf/reg-sub ::view (fn [db _]
                     (get db ::view "form")))
(rf/reg-event-db ::view (fn [db [_ view]]
                               (assoc db ::view view)))



(defn- home-page []
  (let [state (rf/subscribe [::view])]
    (fn []
      [:div
       [:> ant/Tabs
        {:on-change #(rf/dispatch [::view %])
         :active-key @state}
        [:> ant/Tabs.TabPane
         {:tab "Form"
          :key "form"}
         [form/form-page]]
        [:> ant/Tabs.TabPane
         {:tab "Admin"
          :key "admin"}
         [admin/admin-page]]
        [:> ant/Tabs.TabPane
         {:tab "UI"
          :key "ui"}
         [ui/ui-page]]]])))

(defn mount-root []
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
