(ns ez-wire.test-ui
  (:require [ez-wire.ui.table :refer [table]]))




(defn ui-page []
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
                   :last-name "McTest"}]}])
