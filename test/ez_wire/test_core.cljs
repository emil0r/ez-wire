(ns ez-wire.test-core
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [day8.re-frame.test :as rf-test]
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
  (rf-test/run-test-sync
   (let [{:keys [id] :as form} (loginform {} {})
         empty-data {:username nil :password nil}
         full-data {:username "John" :password "Doe"}
         data-form (rf/subscribe [::form/on-valid (:id form)])]
     (testing "id is a uuid"
       (is (string? id)))
     (testing "data-form is empty"
       (is (= @data-form empty-data)))
     (testing "re-frame app-db has no data before some input happens"
       (is (= (get-in @app-db [::form/on-valid id]) empty-data)))
     (do
       (swap! (:data form) assoc :username "John")
       (swap! (:data form) assoc :password "Doe"))
     (testing "data-form is valid"
       (is (= @data-form full-data)))
     (testing "(:data form) has data"
       (is (= @(:data form) full-data)))
     (testing "app-db has references"
       (is (and (not (empty? (::form/error @app-db)))
                (not (empty? (::form/on-valid @app-db))))))
     (testing "app-db no longer has references"
       (rf/dispatch [::form/cleanup id])
       ;; the database should be cleaned up and now be empty
       (do (is (= true (empty? (::form/error @app-db))))
           (is (= true (empty? (::form/on-valid @app-db))))
           (is (= ::form/invalid @data-form)))))))
