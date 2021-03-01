(ns ez-wire.test-ui
  (:require [ez-wire.ui.styling :refer [get-styling] :refer-macros [with-styling]]
            [ez-wire.ui.button :refer [button]]
            [ez-wire.ui.table :refer [table]]
            [ez-wire.ui.pagination :refer [pagination]]))



(def style {:ez-wire.ui.button/button {:css ["primary"]
                                       :style {:background-color "blue"
                                               :color "white"}}
            :ez-wire.ui.table/table {:style {:border "1px dotted black"
                                             :margin "0 0 30px 0"}}})


(defn ui-page []
  (with-styling
    style
    [:div
     [table {:show-columns? true
             :pagination/pp 1
             :pagination? false
             :columns [{:title "Title"
                        :name :title
                        :sort? true
                        :sort/default? true}
                       {:title "First name"
                        :name :first-name
                        :sort? true}
                       {:title "Last name"
                        :name :last-name}
                       {:title "Full name"
                        :fn (fn [{:keys [first-name last-name]}]
                              (str last-name ", " first-name))}]
             :model [{:title "Mr"
                      :first-name "A"
                      :last-name "McTest"}
                     {:title "Mrs"
                      :first-name "B"
                      :last-name "McTest"}
                     {:title "Ms"
                      :first-name "C"
                      :last-name "McTest"}]}]
     [pagination {:length 100}]
     [button {:on-click #(js/alert "hi")}
      "My button"]]))
