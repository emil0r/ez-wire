(ns ez-wire.test-ui
  (:require [ez-wire.ui.table :refer [table]]))




(defn ui-page []
  [table {:show-columns? true
          :pagination/pp 50
          :columns [{:title "Title"
                     :name :title
                     :sort? true
                     :sort/default? true}
                    {:title "First name"
                     :name :first-name
                     :sort? true}
                    {:title "Last name"
                     :name :last-name}]
          :model [{:title "Mr"
                   :first-name "Monkey"
                   :last-name "McMonkey"}
                  {:title "Mrs"
                   :first-name "Donkey"
                   :last-name "McMonkey"}
                  {:title "Ms"
                   :first-name "Hookay"
                   :last-name "McMonkey"}]}])
