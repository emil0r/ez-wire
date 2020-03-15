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
    [:div "current value is " [:strong "'" value "'"] " and it needs to be an integer"]))
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
    :text "My info text"}
   {:element e/input
    :adapter fomantic-adapter
    :placeholder "Second"
    :validation [::my-validation]
    :value ""
    :name :test2}
   {:element e/input
    :adapter fomantic-adapter
    :placeholder "Third"
    :validation [::my-validation]
    :value ""
    :wiring [:tr [:td "bar"] [:td :$field]]
    :name :test3}])


(defn home-page []
  (let [data {:test "foobar"}
        form (testform {} data)]
    (fn []
      [:div
       [:div "Hi there"]
       [:div.left
        [:> e/button
         {:class "alert-button"
          :on-click #(js/alert (pr-str @(:data form)))}
         "Alert data"]]
       [:div.right
        [:div
         [:h2 "My testform [table]"]
         [form/as-table {} form]]
        [:div
         [:h2 "My testform [list]"]
         [form/as-list {} form]]
        [:div
         [:h2 "My testform [paragraph]"]
         [form/as-paragraph {} form]]
        [:div
         [:h2 "My testform [template]"]
         [form/as-template {:template [:div.template :$key
                                       :$label
                                       :$field
                                       :$errors
                                       :$text
                                       :$help]}
          form]]]
       [:div.clear]])))

(defn mount-root []
  (r/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
