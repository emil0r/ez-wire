(ns demo.forms.flight
  (:require [antd :as ant]
            [clojure.spec.alpha :as spec]
            [clojure.string :as str]
            [demo.common :refer [demo-component data]]
            [demo.syntax :as syntax]
            [ez-wire.form :as form]
            [ez-wire.form.helpers :refer [add-external-error
                                          remove-external-error
                                          valid?]]
            [ez-wire.protocols]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [tongue.core :as tongue])
  (:require-macros [ez-wire.form.macros :refer [defform]]
                   [ez-wire.form.validation :refer [defmultivalidation
                                                    defvalidation]]))

;; utility functions to pass between js and cljs

(defn kw->str [kw]
  (subs (str kw) 1))

(defn str->kw [s]
  (keyword s))



;; simple i18n
(rf/reg-sub ::locale (fn [db [_ fallback]]
                       (or (::locale db)
                           fallback)))

(rf/reg-event-db ::locale (fn [db [_ locale]]
                            (assoc db ::locale locale)))

(def locale-sub (rf/subscribe [::locale :en]))


(def dictionary {:en {:username "Username"
                      :password "Password"
                      :ez-wire.form/done "Done"
                      :ez-wire.form.wizard/next "Next"
                      :ez-wire.form.wizard/prev "Previous"
                      :flight/book "Book"
                      :flight/dates "Dates"
                      :flight/invalid-name "Invalid name. Only alphanumeric characters allowed along with whitespace."
                      :flight/first-name "First name"
                      :flight/last-name "Last name"
                      :flight/sex "Sex"
                      :sex/unknown "Unknown"
                      :sex/male "Male"
                      :sex/female "Female"
                      :sex/unicorn "Pink unicorn"}
                 :sv {:username "Användarnamn"
                      :password "Lösenord"
                      :ez-wire.form/done "Klar"
                      :ez-wire.form.wizard/next "Nästa"
                      :ez-wire.form.wizard/prev "Föregående"
                      :flight/book "Boka"
                      :flight/dates "Datum"
                      :flight/invalid-name "Ogiltligt namn. Enbart alfanumeriska bokstäver är tillåtna tillsammans med blanksteg."
                      :flight/first-name "Förnman"
                      :flight/last-name "Efternamn"
                      :flight/sex "Kön"
                      :sex/unknown "Vet ej"
                      :sex/male "Man"
                      :sex/female "Kvinna"
                      :sex/unicorn "Rosa enhörning"}
                 :tongue/fallback :en})
(def translate (tongue/build-translate dictionary))

(defn t [& args]
  (apply translate @locale-sub args))

(extend-protocol ez-wire.protocols/Ii18n
  cljs.core/Keyword
  (t
    ([k] (t k))
    ([k args] (t (cons k args)))))


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
      (let [placeholder (if placeholder (t placeholder))]
        [f (merge {:value @model
                   :placeholder placeholder                   
                   :on-change #(reset! model (-> % .-target .-value))}
                  (select-keys data [:id]))]))))

(defn range-picker-adapater [{:keys [element model] :as field}]
  ;; first level is the initilization phase of the field
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [model value] :as data}]
      (let [current-locale @locale-sub
            current-value (let [[start end] @model]
                            #js [(if start
                                   (js/moment start))
                                 (if end
                                   (js/moment end))])]
        [f {:value current-value
            :locale (if (= current-locale :en)
                      (.-DatePicker ant/locales.en_US)
                      (.-DatePicker ant/locales.sv_SE))
            :on-change #(let [[start end] (map (fn [moment] (.toDate moment)) (js->clj %))]
                          (reset! model [start end]))}]))))

(defn select-adapter [{:keys [element] :as field}]
  (let [f (r/adapt-react-class element)]
    (fn [{:keys [name model value source source/id source/title] :as data}]
      [f {:value (t @model)
          :on-change #(reset! model (str->kw %))
          :filter-option false}
       [:<>
        (doall
         (for [option source]
           ^{:key [name (id option)]}
           [:> ant/Select.Option {:value (kw->str (id option))}
            (t (title option))]))]])))

;; -- validation --
;; We define a spec based validation (provided as default)
;; and return :flight/invalid-name if we get an error
;; :flight/invalid-name is then translated via the ez-wire.protocols/Ii18n
;; protocol which has been extended above to use our own i18n
(defvalidation ::name
  (spec/and string?
            #(not (str/blank? %))
            #(re-find #"^[\s\w]+$" %))
  :flight/invalid-name)


(defvalidation ::dates
  (spec/and some? #(every? some? %))
  ;; instead of a keyword to be translated, we return function which returns
  ;; a reagent component
  (fn [context]
    [:> ant/Alert {:type "warning"
                   :message "Flight needs dates"
                   :showIcon true}]))

(defmultivalidation first-and-last-name-not-equal
  #{:flight/first-name :flight/last-name}
  (fn [{:keys [values]}]
    (let [{:flight/keys [first-name last-name]} values]
      (if (some #(str/blank? %) [first-name last-name])
        true
        (not= first-name last-name))))
  (fn [context]
    [:> ant/Alert {:type "error"
                   :message "Last name cannot be the same as first name"
                   :showIcon true}]))

;; we define our form
(defform flightform
  {}
  [{:element ant/DatePicker.RangePicker
    ;; Adapter will be used to give the final element for the form to use
    :adapter range-picker-adapater
    ;; This is what will be used as the name of the field in the map
    ;; the form produces
    :name :flight/dates
    ;; This is what the field will be validated against
    ;; it goes through the IValidation protocol.
    ;; You can put in anything the validation field, as long as it implements
    ;; the IValidation protocol.
    :validation ::dates}
   {:element ant/Input
    :adapter text-adapter
    :name :flight/first-name
    :validation ::name
    :placeholder :flight/first-name}
   {:element ant/Input
    :adapter text-adapter
    :name :flight/last-name
    :validation [::name first-and-last-name-not-equal]
    :help "You are not allowed to have the same last name as the first name"
    :placeholder :flight/last-name}
   {:element ant/Select
    :adapter select-adapter
    :name :flight/sex
    :value :sex/unknown
    ;; source/id and source/title are sent along into the adapter
    ;; where we use them to coax out the id and the title
    ;; from the source data
    :source/id :id
    :source/title :title
    ;; source is used for the options
    :source [{:id :sex/unknown
              :title :sex/unknown}
             {:id :sex/male
              :title :sex/male}
             {:id :sex/female
              :title :sex/female}
             {:id :sex/unicorn
              :title :sex/unicorn}]}])



(defn langauge-buttons "Switch language for the flightform" []
  (let [current-locale @locale-sub]
    [:div.language-buttons
     [:h2 "These buttons will not trigger a re-render of the labels."]
     [:p "However, a new initialization of a flightform will always utilize the current locale"]
     [:> ant/Button (merge
                     (if (= current-locale :en)
                       {:type "primary"})
                     {:on-click #(rf/dispatch [::locale :en])})
      "English"]
     [:> ant/Button (merge
                     (if (= current-locale :sv)
                       {:type "primary"})
                     {:on-click #(rf/dispatch [::locale :sv])})
      "Svenska"]]))

(defn reset-form [form]
  [:div.reset-form
   [:> ant/Button {:type "danger"
                   :on-click #(form/reset-form! form)}
    "Reset the form"]])

(defn external-error-buttons [form]
  [:div.external-error-buttons
   [:div.columns
    [:div.column>p
     "External errors can be added and removed via helper functions"]]
   [:div.columns
    [:div.column
     [:> ant/Button {:type "primary"
                     :on-click #(add-external-error form :flight/sex :name-of-my-error "This is an external error that has been added by the click of a button." true)}
      "Add external error"]]
    [:div.column
     [:> ant/Button {:type "primary"
                     :on-click #(remove-external-error form :flight/sex :name-of-my-error)}
      "Remove external error"]]]])

(defn component []
  (let [;; initialized flightform with the start date for :flight/dates
        ;; as today
        form (flightform {} {:flight/dates [(js/Date.) nil]})
        ;; subscribe to our form
        data-form (rf/subscribe [::form/on-valid (:id form)])]
    (fn []
      [:div
       [:div.columns
        [:div.column
         [langauge-buttons]]
        [:div.column
         [reset-form form]]]
       [:div.columns
        [:div.column
         [form/as-table {} form]
         [:> ant/Button
          {:type "primary"
           ;; valid? is a helper function that either takes a value, a form
           ;; or a reagent RAtom/RCursor/Reaction
           :disabled (not (valid? data-form))
           :on-click #(js/alert (pr-str @data-form))}
          (t :flight/book)]]
        [:div.column
         [external-error-buttons form]
         [:h4.mt-5 "Data in the form"]
         [data @data-form]]]])))

(defn form-flight []
  [:div.form-flight
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/flight.cljs")}]])
