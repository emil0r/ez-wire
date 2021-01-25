(ns ez-wire.test-ui
  (:require [ez-wire.ui.table :refer [table]]))




(defn ui-page []
  [table {:columns [{:title "Title"
                     :name :title}
                    {:title "First name"
                     :name :first-name}
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
