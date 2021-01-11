(ns demo.forms.wired
  (:require [antd :as ant]
            [demo.common :refer [demo-component data]]
            [demo.syntax :as syntax]
            [demo.forms.flight :refer [flightform]]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid?]]
            [re-frame.core :as rf]))


(defn component []
  (let [;; initiate our form as a wizard
        wired-form (flightform {} {})
        ;; subscribe to our form
        data-form (rf/subscribe [::form/on-valid (:id wired-form)])]
    (fn []
      [:div
       [:div.columns
        [:div.column
         [form/as-wire {:wiring [:table.wired>tbody
                                 [:tr.sex
                                  [:th :$flight/sex.label]
                                  [:td
                                   :$flight/sex.field
                                   :$flight/sex.errors]]
                                 [:tr.dates>td {:col-span 2}
                                  :$flight/dates.field
                                  :$flight/dates.errors]
                                 [:tr.names
                                  [:td
                                   :$flight/first-name.field
                                   :$flight/first-name.errors]
                                  [:td
                                   :$flight/last-name.field
                                   :$flight/last-name.errors]]]}
          wired-form]
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

(defn form-wired []
  [:div.form-wired
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/wired.cljs")}]])

