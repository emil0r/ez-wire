(ns demo.forms.order
  (:require [antd :as ant]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [demo.common :refer [demo-component data]]
            [demo.syntax :as syntax]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [valid?]]
            [ez-wire.protocols]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [tongue.core :as tongue])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defvalidation]]))


(defn kw->str [kw]
  (subs (str kw) 1))

(defn str->kw [s]
  (keyword s))


;; -- adapaters --
;; Each adapater takes an element from antd
;; There is an initialization phase for an adapter
;; where the field is sent, without the extra fields
;; ez-wire.form adds.
;; The inner function is what returns our reagent
;; component that is then used by ez-wire.form

(defn text-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value placeholder] :as data}]
      [f (merge {:value @model
                 :placeholder placeholder                   
                 :on-change #(reset! model (-> % .-target .-value))}
                (select-keys data [:id]))])))

(defn select-adapter [{:keys [element keywordize?] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [name model value source source/id source/title] :as data}]
      [f {:value (if keywordize?
                   (kw->str @model)
                   @model)
          :on-change #(reset! model (if keywordize?
                                      (str->kw %)
                                      %))
          :filter-option false}
       [:<>
        (doall
         (for [option source]
           ^{:key [name (id option)]}
           [:> ant/Select.Option {:value (if keywordize?
                                           (kw->str (id option))
                                           (id option))}
            (title option)]))]])))


(defn value-adapter [{:keys [source]}]
  (fn [{:keys [model] :as data}]
    [:div (source @model)]))


(defform orderform
  {:branch/branching? true
   :branch/branches {:order/reader (fn [{:keys [value]}]
                                     (let [value (str->kw value)]
                                       ;; value will always be set to shipping-cost
                                       (case value
                                         ;; for the kindle, show the cover field, update the cover field and set the value for the model
                                         :kindle {:show-fields [:order/cover]
                                                  :fields {:order/cover {:model :black-cover
                                                                         :source [{:id :black-cover
                                                                                   :title "Black"}
                                                                                  {:id :grey-cover
                                                                                   :title "Grey"}
                                                                                  {:id :chestnut-cover
                                                                                   :title "Chestnut"}]}
                                                           :order/shipping-cost {:model value}}}
                                         ;; same for sony, but change the types of covers
                                         :sony {:show-fields [:order/cover]
                                                :fields {:order/cover {:model :metallic-cover
                                                                       :source [{:id :metallic-cover
                                                                                 :title "Metallic"}
                                                                                {:id :silver-cover
                                                                                 :title "Silver"}]}
                                                         :order/shipping-cost {:model value}}}
                                         ;; hide the cover for all the other types of readers
                                         {:hide-fields [:order/cover]
                                          :fields {:order/shipping-cost {:model value}}})))}}
  [{:element ant/Input
    :adapter text-adapter
    :name :order/first-name
    :label "First name"}
   {:element ant/Input
    :adapter text-adapter
    :name :order/last-name
    :label "Last name"}
   {:element ant/Select
    :adapter select-adapter
    :keywordize? true
    :source/id :id
    :source/title :title
    :value :kindle
    :source [{:id :kindle
              :title "Kindle"}
             {:id :kobo-aura
              :title "Kobo Aura Edition 2"}
             {:id :kobo-h20
              :title "Kobo H20 Edition 2"}
             {:id :sony
              :title "Sony DPT-RP1/B"}]
    :name :order/reader
    :label "EBook Reader"}
   {:element ant/Select
    :adapter select-adapter
    :keywordize? true
    :source/id :id
    :source/title :title
    :value :black-cover
    :source [{:id :black-cover
              :title "Black"}
             {:id :grey-cover
              :title "Grey"}
             {:id :chestnut-cover
              :title "Chestnut"}]
    :name :order/cover
    :label "Cover"}
   ;; automatically selected based on which reader has been picked
   ;; the shipping cost automatically follows and cannot be deselected
   {:element nil
    :adapter value-adapter
    :source {:kindle "$0"
             :kobo-aura "$5"
             :kobo-h20 "$5"
             :sony "$7"}
    :name :order/shipping-cost
    :label "Shipping cost"}])


(defn component []
  (r/with-let [form (orderform {} {})
               data-form (rf/subscribe [::form/on-valid (:id form)])]
    [:div
     [form/as-table {} form]
     [:> ant/Button
      {:type "primary"
       :disabled (not (valid? data-form))
       :on-click #(js/alert (pr-str @data-form))}
      "Place order"]]))


(defn form-order []
  [:div.form-order
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/order.cljs")}]])
