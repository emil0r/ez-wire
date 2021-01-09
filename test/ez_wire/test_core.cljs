(ns ez-wire.test-core
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [ez-wire.form :as form]
            [re-frame.core :as rf]
            [re-frame.db :refer [app-db]])
  (:require-macros [ez-wire.form.macros :refer [defform]]))

(defn input-text [{:keys [model placeholder]}]
  [:input.input {:type :text
                 :placeholder placeholder
                 :value @model
                 :on-change #(reset! model (-> % .-target .-value))}])

(defn input-password [{:keys [model placeholder]}]
  [:input.input {:type :password
                 :placeholder placeholder
                 :value @model
                 :on-change #(reset! model (-> % .-target .-value))}])

(defform loginform
  {}
  [{:element input-text
    :placeholder "Input your password"
    :name :username}
   {:element input-password
    :placeholder "Input your password"
    :name :password}])


(deftest test-form
  (let [{:keys [id] :as form} (loginform {} {})
        data-form (rf/subscribe [::form/on-valid (:id form)])]
    (testing "id is a uuid"
      (is (string? id)))
    (testing "data-form is empty"
      (is (= @data-form :ez-wire.form/invalid)))
    (testing "re-frame app-db has no data before some input happens"
      (is (nil? (get-in @app-db [::form/on-valid id]))))
    (do
      (swap! (:data form) assoc :username "John")
      (swap! (:data form) assoc :password "Doe"))
    (testing "data-form is valid"
      (is (= @data-form {:username "John"
                         :password "Doe"})))
    (testing "(:data form) has data"
      (is (= @(:data form) {:username "John"
                            :password "Doe"})))
    (testing "app-db has references"
      (is (nil? form)))))
