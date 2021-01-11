(ns demo.forms.wizard
  (:require [antd :as ant]
            [demo.common :refer [demo-component data]]
            [demo.syntax :as syntax]
            [demo.forms.flight :refer [flightform]]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid?]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn component []
  (let [;; initiate our form as a wizard
        form (flightform {:label? false
                          :render :wizard
                          :wizard {:button/props {:type "primary"}
                                   :button/element (r/adapt-react-class ant/Button)
                                   :steps [{:fields [:flight/dates]
                                            :legend [:h3 "Select your dates"]}
                                           {:fields [:flight/first-name
                                                     :flight/last-name]
                                            :legend [:h3 "Fill in your name please"]}
                                           {:fields [:flight/sex]
                                            :legend [:h3 "Sex"]}]}}
                         {})
        ;; need the current step of the wizard
        wizard-current-step (rf/subscribe [:ez-wire.form.wizard/current-step (:id form)])
        ;; subscribe to our form
        data-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div
       [:div.columns
        [:div.column
         [form/as-paragraph {} form]
         [:> ant/Button
          {:type "primary"
           ;; valid? is a helper function that either takes a value, a form
           ;; or a reagent RAtom/RCursor/Reaction
           :disabled (not (valid? data-form))
           :on-click #(js/alert (pr-str @data-form))}
          "Book"]]
        [:div.column
         [:h4 "Current step"]
         [:p @wizard-current-step]
         [:h4.mt-5 "Data in the form"]
         [data @data-form]]]])))

(defn form-wizard []
  [:div.form-wizard
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/wizard.cljs")}]])
