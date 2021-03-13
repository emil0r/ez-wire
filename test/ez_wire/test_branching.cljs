(ns ez-wire.test-branching
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

(defn input-dropdown [{:keys [model options]}]
  [:select {:value @model
            :on-change #(let [value (-> % .-target .-value)]
                          (reset! model value))}
   (for [option options]
     ^{:key option}
     [:option option])])


(defform userform
  {:branch/branching? true
   :branch/branches {:type (fn [form k value]
                             (condp = value
                               "admin"
                               {:show-fields :all
                                :hide-fields [:role :first-name :last-name]}
                               "user"
                               {:show-fields [:first-name :last-name]
                                :hide-fields :all}
                               "staff"
                               {:show-fields []}))}}
  [{:element input-text
    :placeholder "Input your username"
    :name :username}
   {:element input-dropdown
    :value "admin"
    :options ["admin"
              "user"
              "staff"]
    :name :type}
   {:element input-dropdown
    :option ["shop"
             "user"
             "inventory"]
    :active? false
    :name :role}
   {:element input-text
    :placeholder "First name"
    :active? false
    :name :first-name}
   {:element input-text
    :placeholder "Last name"
    :active? false
    :name :last-name}
   {:element input-password
    :placeholder "Password"
    :active? false
    :name :password}])

(defn- get-active-fields [form]
  (->> form
       :fields
       (map second)
       (filter (fn [field]
                 (true? @(:active? field))))
       (map :name)
       (into #{})))

(defn- set-field! [form k value]
  (swap! form assoc-in [:data k] value))


(deftest branching
  (rf-test/run-test-sync
   (let [myform (userform {} {})]
     (testing "default active fields"
       (is (= (get-active-fields myform) #{:username :type}))))))
