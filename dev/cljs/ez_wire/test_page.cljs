(ns ez-wire.test-page
  (:require [antd :as ant]
            [ez-wire.test-admin :as admin]
            [ez-wire.test-form :as form]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(enable-console-print!)

(rf/reg-sub ::view (fn [db _]
                     (get db ::view :form)))
(rf/reg-event-db ::flip-view (fn [db _]
                               (let [view (get db ::view :form)]
                                 (if (= view :form)
                                   (assoc db ::view :admin)
                                   (assoc db ::view :form)))))



(defn- home-page []
  (let [state (rf/subscribe [::view])]
    (fn []
      [:div
       (let [text (if (= @state :form)
                    "Flip to admin"
                    "Flip to form")]
         [:div
          [:> ant/Button {:type "primary"
                          :on-click #(rf/dispatch [::flip-view])}
           text]])
       (if (= @state :form)
         [form/form-page]
         [admin/admin-page])])))

(defn mount-root []
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
