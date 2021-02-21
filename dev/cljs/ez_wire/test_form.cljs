(ns ez-wire.test-form
  (:require [antd :as ant]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [cljs.pprint]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [re-frame.db])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defvalidation]]))

(enable-console-print!)

(defn text-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      [f (merge {:defaultValue value
                 :on-change #(reset! model (-> % .-target .-value))}
                (select-keys data [:id :placeholder]))])))

(defn integer-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      [f (merge {:defaultValue (str value)
                 :on-change #(let [value (-> % .-target .-value)
                                   int-value (js/parseInt value)]
                               (if-not (js/isNaN int-value)
                                 (reset! model int-value)
                                 (reset! model value)))}
                (select-keys data [:id :placeholder]))])))


(defvalidation ::int
  (spec/or :int int? :blank str/blank?)
  (fn [{:keys [value]}]
    [:div "current value is " [:strong "'" value "'"] " and it needs to be an integer"]))
(defvalidation ::percent
  #(and (>= % 0) (<= % 100))
  (fn [{:keys [value]}]
    [:div "current value is " [:strong "'" value "'"] " and it needs to be between 0 and 100"]))
(defvalidation ::long-string
  (spec/and string?
            #(> (count %) 2))
  "Need to be a string and more than two characters")


(defform testform
  {}
  [{:element ant/Input
    :adapter text-adapter
    :placeholder "foobar"
    :validation [::long-string]
    :value "test"
    :name :test1
    :label "My test input"
    :help "My help text for test input"
    :text "My info text"}
   {:element ant/Input
    :adapter integer-adapter
    :placeholder "Second (numbers)"
    :help "This input field takes only numbers"
    :value ""
    :validation [::int ::percent]
    :name :test2}
   {:element ant/Input
    :adapter text-adapter
    :placeholder "Third"
    :wiring [:tr [:td "bar"] [:td :$field]]
    :validation [::long-string]
    :name :test3}
   {:element ant/Input
    :adapter text-adapter
    :placeholder ""
    :text "External error should show up even with no other validation added"
    :name :test4}])


(defn forms []
  (let [data {:test1 "Elsa"
              :test2 "asdf"}
        form (testform {} data)
        wizard-form (testform {:render :wizard
                               :wizard {:steps [{:fields [:test1]
                                                 :legend [:h3 "Part 1"]}
                                                {:fields [:test2]
                                                 :legend [:h3 "Part 2"]}
                                                {:fields [:test3 :test4]
                                                 :legend [:h3 "Part 3"]}]}}
                              data)
        wizard-current-step (rf/subscribe [:ez-wire.form.wizard/current-step (:id wizard-form)])
        valid-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div
       [:div "Hi there"]
       [:div.left
        [:div
         [:> ant/Button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test1 :foo "This is external error A" true)}
          "Add external passing error to :test1"]
         [:> ant/Button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test1 :bar "This is external error B" false)}
          "Add external non-passing error to :test1"]
         [:> ant/Button
          {:class "remove-error"
           :on-click #(helpers/remove-external-error form :test1 :bar)}
          "Remove external non-passing error to :test1"]
         
         [:> ant/Button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test4 :foo "This is an external error" true)}
          "Add external passing error to :test4"]

         [:> ant/Button
          {:class "reset-form"
           :on-click #(form/reset-form! form)}
          "Reset form"]]
        [:div
         [:> ant/Button
          {:class "alert-button"
           :on-click #(js/alert (pr-str @(:data form)))}
          "Alert data"]
         [:> ant/Button
          {:class "valid-button"
           :on-click #(js/alert (pr-str @valid-form))}
          "Valid form?"]]
        [:div {:class "internal-info"}
         [:h2 "Internal state of the form"]
         (when @(:data form)
           (pr-str form))]]
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
          form]]
        [:div
         [:h2 "My testform [wire]"]
         [form/as-wire {:wiring
                        [:div.wire
                         [:div.number1
                          :$test.label
                          :$test.field
                          :$test.errors]
                         [:div.number2
                          :$test2.label
                          :$test2.field
                          :$test2.errors]]}
          form]]
        [:div
         [:h2 "My wizard testform [table]"]
         [:div "Step " @wizard-current-step]
         [form/as-table {} wizard-form]]]
       [:div.clear]])))

(defn form-page []
  (let [show-forms? (r/atom true)]
    (fn []
      [:div
       [:> ant/Button {:on-click #(reset! show-forms? (not @show-forms?))}
        "Toggle form page"]
       (if @show-forms?
         [forms]
         [:pre (with-out-str (cljs.pprint/pprint @re-frame.db/app-db))])])))