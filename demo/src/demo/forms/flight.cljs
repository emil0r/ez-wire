(ns demo.forms.flight
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

(rf/reg-sub ::locale (fn [db [_ fallback]]
                       (or (::locale db)
                           fallback)))

(rf/reg-event-db ::locale (fn [db [_ locale]]
                            (assoc db ::locale locale)))

(def locale-sub (rf/subscribe [::locale :en]))


(def dictionary {:en {:username "Username"
                      :password "Password"
                      :flight/book "Book"
                      :flight/dates "Dates"
                      :flight/invalid-name "Invalid name. Only alphanumeric characters allowed along with whitespace."
                      :flight/first-name "First name"
                      :flight/last-name "Last name"}
                 :sv {:username "Användarnamn"
                      :password "Lösenord"
                      :flight/book "Boka"
                      :flight/dates "Datum"
                      :flight/invalid-name "Ogiltligt namn. Enbart alfanumeriska bokstäver är tillåtna tillsammans med blanksteg."
                      :flight/first-name "Förnman"
                      :flight/last-name "Efternamn"}
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
        [f (merge {:default-value (str value)
                   :placeholder placeholder                   
                   :on-change #(reset! model (-> % .-target .-value))}
                  (select-keys data [:id]))]))))

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
          (let [current-locale @locale-sub]
            [f {:default-value default-value
                :locale (if (= current-locale :en)
                          (.-DatePicker ant/locales.en_US)
                          (.-DatePicker ant/locales.sv_SE))
                :on-change #(let [[start end] (map (fn [moment] (.toDate moment)) (js->clj %))]
                              (reset! model [start end]))}]))))))


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

(defform flightform
  {}
  [{:element ant/DatePicker.RangePicker
    :adapter range-picker-adapater
    :name :flight/dates
    :validation ::dates}
   {:element ant/Input
    :adapter text-adapter
    :name :flight/first-name
    :validation ::name
    :placeholder :flight/first-name}
   {:element ant/Input
    :adapter text-adapter
    :name :flight/last-name
    :validation ::name
    :placeholder :flight/last-name}])



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
         [langauge-buttons]]]
       [:div.columns
        [:div.column
         [form/as-table {} form]
         [:> ant/Button
          {:type "primary"
           :disabled (not (valid? data-form))
           :on-click #(js/alert (pr-str @data-form))}
          (t :flight/book)]]
        [:div.column
         [:h4 "Data in the form"]
         [data @data-form]]]])))

(defn form-flight []
  [:div.form-flight
   [demo-component {:comp component
                    :src (syntax/src-of nil "demo/forms/flight.cljs")}]])
