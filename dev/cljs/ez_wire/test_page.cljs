(ns ez-wire.test-page
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [reagent.core :as r]
            [ez-wire.element :as e]
            [ez-wire.form :as form :refer [defform]]
            [ez-wire.form.validation :as validation]
            [ez-wire.util :as util]
            [re-frame.core :as rf])
  (:require-macros [ez-wire.form.macros :refer [defform]]))

(enable-console-print!)

(defn fomantic-adapter [{:keys [element name] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [value model] :as data}]
      [f (merge {:value @model
                 :on-change #(reset! model (-> % .-target .-value))}
                (select-keys data [:id :placeholder]))])))

(validation/def ::my-validation
  (spec/or :int int? :blank str/blank?)
  (fn [{:keys [value]}]
    (str "current value is " value)))
(validation/def ::stupid
  (spec/and string?
            #(> (count %) 2))
  "Need to be a string and more than two characters")


(defform testform
  {}
  [{:element e/input
    :adapter fomantic-adapter
    :placeholder "foobar"
    :validation [::my-validation ::stupid]
    :value "test"
    :name :test
    :label "My test input"
    :help "My help text for test input"
    :text "My info text"}])


(defn home-page []
  (let [data {:test "foobar"}
        form (testform {} data)]
    (fn []
      [:div
       [:div "Hi there"]
       
       [:div
        [:h2 "My testform"]
        [form/as-table {} form]
        [:> e/button {:on-click #(js/alert (pr-str @(:data form)))} "Alert data"]]])))

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
