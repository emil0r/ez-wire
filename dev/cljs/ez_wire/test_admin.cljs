(ns ez-wire.test-admin
  (:require [ez-wire.admin :as admin]
            [re-frame.core :as rf]))

(defn user-display-fn [user]
  (str "{"
       (:user/username user)
       "} ["
       (:user/first-name user)
       ", "
       (:user/last-name user)
       "]"))

(defn init! []
  (admin/register-module
   {:module/key :animals
    :module/name "Authentication"
    :module/entities {:user {:entity/name "User"
                             :entity/interface {:display {:user/username {:name "My overridden name"
                                                                          :display-fn user-display-fn
                                                                          :link? true
                                                                          :sort/key :user/id}
                                                          :user/email {:sort/key :user/email}
                                                          :user/active? {:sort/key :user/active?}}
                                                ;; which order do we display the display in?
                                                :order [:user/username :user/email :user/active?]
                                                ;; default sort key
                                                :sort/key :user/id}
                             :entity/fields {:user/username {:field/type :text
                                                             :field/label "Username"}
                                             :user/password {:field/type :password
                                                             :field/label "Password"}
                                             :user/email {:field/type :email
                                                          :field/label "Email"
                                                          :field/validation :ez-wire.validation/email}
                                             :user/active? {:field/type :checkbox
                                                            :field/label "Active?"}
                                             :user/first-name {:field/type :text
                                                               :field/label "First name"}
                                             :user/last-name {:field/type :text
                                                              :field/label "Last name"}
                                             :user/roles {:field/type :select
                                                          :prop/multiple? true
                                                          :field/label "Roles"}
                                             :user/groups {:field/type :select
                                                           :prop/multiple? true
                                                           :field/label "Groups"}}}
                      :group {:entity/name "Group"
                              ;;:entity/fields
                              }
                      :role {:entity/name "Role"
                             ;;:entity/fields
                             }}
    :module/display {:entities/order [:user
                                      :group
                                      :role]}}))

(defn debug []
  (let [navigation (rf/subscribe [:ez-wire.admin/navigation])]
    [:pre (pr-str @navigation)]))

(defn admin-page []
  (init!)
  [:div
   [admin/admin-index]
   [debug]])
