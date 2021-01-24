(ns ez-wire.admin.view
  (:require [ez-wire.admin.view.entity :as view-entity]
            [ez-wire.admin.view.module :as view-module]
            [ez-wire.admin.helpers :refer [navigate home home?]]
            [ez-wire.protocols :refer [t]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn- breadcrumbs [module entity entity-id]
  (r/with-let [modules (rf/subscribe [:ez-wire.admin/modules])]
    [:div.ez-wire-admin-breadcrumbs
     (if (home? module)
       [:span (t :ez-wire/admin)]
       (let [module* (get @modules module)
             entity-name (get-in module* [:module/entities entity :entity/name])
             parts (->> [{:part :ez-wire/admin
                          :navigation #(navigate)}
                         {:part (:module/name module*)
                          :navigation #(navigate module)}
                         {:part entity-name
                          :navigation #(navigate module entity)}
                         {:part entity-id}]
                        (remove #(nil? (:part %))))
             sep [:span " ›› "]]
         [:div.ez-wire-admin-navigation
          (interpose
           sep
           (map (fn [{:keys [part navigation]}]
                  [:a {:href "#"
                       :on-click navigation}
                   (t part)])
                (butlast parts)))
          sep
          [:span (-> parts last :part t)]]))]))


(defn index []
  (r/with-let [navigation (rf/subscribe [:ez-wire.admin/navigation])]
    [:div.ez-wire-admin
     (let [{:keys [module entity entity-id]} @navigation]
       (list
        [breadcrumbs module entity entity-id]
        
        (cond (home? module)
              [view-module/list-modules]

              ;; module
              ;; [view-module/list-single-module module]

              (and module entity)
              [view-entity/list-entity module entity]

              ;; catch all
              :else
              [view-module/list-modules])))]))
