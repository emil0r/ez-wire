(ns ez-wire.test-page
  (:require [antd :as ant]
            [ez-wire.test-admin :as admin]
            [ez-wire.test-form :as form]
            [reagent.core :as r]
            [reagent.dom :as rdom]))

(enable-console-print!)


(defn flip-state [state]
  (if (= @state :form)
    (reset! state :admin)
    (reset! state :form)))



(defn- home-page []
  (let [state (r/atom :form)]
    (fn []
      [:div
       (let [text (if (= @state :form)
                    "Flip to admin"
                    "Flip to form")]
         [:div
          [:> ant/Button {:type "primary"
                          :on-click #(flip-state state)}
           text]])
       (if (= @state :form)
         [form/form-page]
         [admin/admin-page])])))

(defn mount-root []
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
