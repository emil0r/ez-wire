(ns demo.forms.flight
  (:require [antd :as ant :refer [Button
                                  DatePicker
                                  RangePicker
                                  DatePicker.RangePicker
                                  Input]]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid?]]
            [reagent.core :as r]
            [re-frame.core :as rf])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defvalidation]]))


(defn text-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      [f (merge {:default-value (str value)
                 :on-change #(reset! model (-> % .-target .-value))}
                (select-keys data [:id :placeholder]))])))

(defn range-picker-adapater [{:keys [element model] :as field}]
  ;; first level is the initilization phase of the field
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      ;; second level is a Form-2 reagent component
      (let [default-value (when value
                            (let [[start end] value]
                              #js [(if start
                                     (js/moment start))
                                   (if end
                                     (js/moment end))]))]
        (fn [{:keys [model] :as data}]
          ;; inner part of the Form-2 reagent component
          [f {:default-value default-value
              :on-change #(let [[start end] (map (fn [moment] (.toDate moment)) (js->clj %))]
                            (reset! model [start end]))}])))))


(defvalidation ::name
  (spec/and string?
            #(not (str/blank? %))
            #(re-find #"^[\s\w]+$" %))
  "asdf")

(defform flightform
  {}
  [{:element ant/DatePicker.RangePicker
    :adapter range-picker-adapater
    :name :dates}
   {:element Input
    :adapter text-adapter
    :name :first-name
    :validation ::name
    :placeholder :flight/first-name}
   {:element Input
    :adapter text-adapter
    :name :last-name
    :validation ::name
    :placeholder :flight/last-name}])



(defn form-flight []
  (let [form (flightform {} {:dates [(js/Date.) nil]})
        data-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div.columns
       [:div.column
        [form/as-table {} form]]
       [:div.column
        [:h4 "Data in the form"]
        [:pre (with-out-str (cljs.pprint/pprint @data-form))]]])))
