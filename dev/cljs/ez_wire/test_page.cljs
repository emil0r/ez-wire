(ns ez-wire.test-page
  (:require [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [cljs.pprint]
            [ez-wire.element :as e]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [re-frame.db])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defmultivalidation
                                                    defvalidation]]))

(enable-console-print!)

(defn text-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      [f (merge {:value (or @model value)
                 :on-change #(reset! model (-> % .-target .-value))}
                (select-keys data [:id :placeholder]))])))

(defn integer-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      [f (merge {:value (str (or @model value))
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

(defmultivalidation more-than-one
  #{:test3 :test4}
  (fn [{:keys [values form]}]
    (every? #(not (str/blank? %)) (vals values)))
  (fn [_]
    [:div "You need more than one value present"]))

(defform testform
  {:branch/branching? true
   :branch/branches {:test5 (fn [form k value]
                              (condp = value
                                "all"
                                {:show-ks :all}
                                "one"
                                {:show-ks [:test1 :test2 :test6 :test7]
                                 :hide-ks :all
                                 :fields {:test1 {:placeholder "foo"
                                                  :help "My new help text"}
                                          :test2 {:placeholder "bar"
                                                  :help "I was not here before"}}}
                                "two"
                                {:show-ks [:test3 :test4]
                                 :hide-ks :all}))}}
  [{:element e/input
    :adapter text-adapter
    :placeholder "foobar"
    :validation [::long-string]
    :value "test"
    :name :test1
    :label "My test input"
    :help "My help text for test input"
    :text "My info text"}
   {:element e/input
    :adapter integer-adapter
    :placeholder "Second (numbers)"
    :help "This input field takes only numbers"
    :value ""
    :validation [::int ::percent]
    :name :test2}
   {:element e/input
    :adapter text-adapter
    :placeholder "Third"
    :wiring [:tr [:td "bar"] [:td :$field :$errors]]
    :validation [::long-string]
    :name :test3}
   {:element e/input
    :adapter text-adapter
    :placeholder ""
    :text "External error should show up even with no other validation added"
    :name :test4
    :validation [::long-string more-than-one]}
   {:element e/dropdown
    :options ["all" "one" "two"]
    :name :test5}
   {:element e/text
    :name :test6
    :active? false
    :value "This is test 6"}
   {:element e/text
    :name :test7
    :active? false
    :value "This is test 7"}])


(defn form-page []
  (let [data {:test1 "Elsa"
              :test2 "asdf"}
        form (testform {} data)
        wizard-form (testform {:render :wizard
                               :wizard {:steps [{:fields [:test1]
                                                 :legend [:h3 "Part 1"]}
                                                {:fields [:test2]
                                                 :legend [:h3 "Part 2"]}
                                                {:fields [:test3 :test4 :test5 :test6 :test7]
                                                 :legend [:h3 "Part 3"]}]}}
                              data)
        wizard-current-step (rf/subscribe [:ez-wire.form.wizard/current-step (:id wizard-form)])
        valid-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div
       [:div "Hi there"]
       [:div.left
        [:div
         [:> e/button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test1 :foo "This is external error A" true)}
          "Add external passing error to :test1"]
         [:> e/button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test1 :bar "This is external error B" false)}
          "Add external non-passing error to :test1"]
         [:> e/button
          {:class "remove-error"
           :on-click #(helpers/remove-external-error form :test1 :bar)}
          "Remove external non-passing error to :test1"]
         
         [:> e/button
          {:class "add-error"
           :on-click #(helpers/add-external-error form :test4 :foo "This is an external error" true)}
          "Add external passing error to :test4"]

         [:> e/button
          {:class "reset-form"
           :on-click #(form/reset-form! form)}
          "Reset form"]]
        [:div
         [:> e/button
          {:class "alert-button"
           :on-click #(js/alert (pr-str @(:data form)))}
          "Alert data"]
         [:> e/button
          {:class "valid-button"
           :on-click #(js/alert (pr-str @valid-form))}
          "Valid form?"]
         [:> e/button
          {:class "error-button"
           :on-click #(js/alert (pr-str (:errors form)))}
          "Current errors"]]
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
         [form/as-template {:template/element :span
                            :template [:div.template :$key
                                       :$label
                                       :$field
                                       :$errors
                                       :$text
                                       :$help]}
          form]]
        [:div
         [:h2 "My testform [wire]"]
         [form/as-wire {:wiring/element :span
                        :class "foobar"
                        :wiring
                        [:div.wire
                         [:$test1.branch
                          [:div.number1
                           :$test1.label
                           :$test1.field
                           :$test1.errors]]
                         [:$test2.branch
                          [:div.number2
                           :$test2.label
                           :$test2.field
                           :$test2.errors]]
                         [:$test3.branch
                          [:div.number3
                           :$test3.label
                           :$test3.field
                           :$test3.errors]]
                         [:$test4.branch
                          [:div.number4
                           :$test4.label
                           :$test4.field
                           :$test4.errors]]
                         [:$test5.branch
                          [:div.number5
                           :$test5.label
                           :$test5.field
                           :$test5.errors]]
                         [:$test6.branch
                          [:div.number6
                           :$test6.label
                           :$test6.field
                           :$test6.errors]]
                         [:$test7.branch
                          [:div.number7
                           :$test7.label
                           :$test7.field
                           :$test7.errors]]]}
          form]]
        [:div
         [:h2 "My wizard testform [table]"]
         [:div "Step " @wizard-current-step]
         [form/as-table {} wizard-form]]]
       [:div.clear]])))

(defn home-page []
  (let [show-form-page? (r/atom true)]
    (fn []
      [:div
       [:> e/button {:on-click #(reset! show-form-page? (not @show-form-page?))}
        "Toggle form page"]
       (if @show-form-page?
         [form-page]
         [:pre (with-out-str (cljs.pprint/pprint @re-frame.db/app-db))])])))

(defn mount-root []
  (rdom/render [home-page] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
