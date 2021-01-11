(ns demo.forms.templated
  (:require [antd :as ant]
            [demo.common :refer [demo-component data]]
            [demo.syntax :as syntax]
            [demo.forms.flight :refer [flightform]]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid?]]
            [re-frame.core :as rf]))


(defn component []
  (let [;; initiate our form as a wizard
        templated-form (flightform {} {})
        ;; subscribe to our form
        data-form (rf/subscribe [::form/on-valid (:id templated-form)])]
    (fn []
      [:div
       [:div.columns
        [:div.column
         [form/as-template {:template [:div.template
                                       :$label
                                       [:div
                                        :$field
                                        :$errors]]}
          templated-form]
         [:> ant/Button
          {:type "primary"
           ;; valid? is a helper function that either takes a value, a form
           ;; or a reagent RAtom/RCursor/Reaction
           :disabled (not (valid? data-form))
           :on-click #(js/alert (pr-str @data-form))}
          "Book"]]
        [:div.column
         [:h4.mt-5 "Data in the form"]
         [data @data-form]]]])))

(defn form-templated []
  [:div.form-templated
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/templated.cljs")}]])
