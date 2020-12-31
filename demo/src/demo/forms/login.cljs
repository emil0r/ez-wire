(ns demo.forms.login
  (:require [demo.common :refer [demo-component code]]
            [demo.syntax :as syntax]
            [ez-wire.form :as form]
            [reagent.core :as r]
            [re-frame.core :as rf])
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

(defn show-inputs []
  [:<>
   [input-text {:model (r/atom nil) :placeholder "My custom placeholder 1"}]
   [input-password {:model (r/atom nil) :placeholder "My custom placeholder 2"}]])

(defn component []
  (let [form (loginform {} nil)
        data-form (rf/subscribe [:ez-wire.form/on-valid (:id form)])]
    (fn []
      [:div
       [:p "loginform is initiated with options and initial data."]
       [:p "Once initiated, we display it as a sequence of paragraphs."]
       [form/as-paragraph {} form]
       [:p "We subscribe to the data from ez-wire.form using re-frame"]
       [:pre (pr-str @data-form)]])))

(defn form-login []
  [:div.form-login
   [demo-component {:comp show-inputs
                    :src (syntax/src-of [:input-text :input-password :show-inputs])}]
   [demo-component {:comp component
                    :src (syntax/src-of [:loginform :component])}]])
