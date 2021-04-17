(ns ez-wire.form.wizard
  (:require [ez-wire.form.common :as common]
            [ez-wire.form.elements :as elements]
            [ez-wire.form.helpers :as helpers]
            [ez-wire.protocols :refer [t]]
            [ez-wire.util :as util]
            [reagent.core :as r]
            [re-frame.core :as rf]))

(defn show-form-fn
  "Show the form with the fn chosen, can send in a new field-ks"
  ([form-fn {:keys [params form content]}]
   [form-fn params form content])
  ([form-fn current-step {:keys [form] :as args}]
   (swap! (:wizard form) assoc :current-step current-step)
   [show-form-fn form-fn args]))

(defn render-navigation [{:keys [step last-step? first-step? max-steps form button css button-props]}]
  (r/with-let [data (rf/subscribe [:ez-wire.form/on-valid (:id form)])
               valid-fn (get-in form [:options :wizard :valid-fn])]
    (let [valid? (helpers/valid? data)]
      [:div {:class (get css :pagination "pagination")}
       [:div {:class (get css :prev "prev")}
        [button {:disabled first-step?
                 :class (get css :button "btn primary")
                 :on-click #(reset! step (max 0 (dec @step)))}
         (t ::prev)]]
       [:div {:class (get css :next "next")}
        [button (merge
                 button-props
                 {:disabled (and last-step? (not valid?))
                  :class (get css :button)
                  :on-click #(cond (not last-step?)
                                   (reset! step (min max-steps (inc @step)))
                                   
                                   (and last-step? valid? valid-fn)
                                   (valid-fn @data)
                                   
                                   :else
                                   nil)})
         (if (and last-step? valid?)
           (t :ez-wire.form/done)
           (t ::next))]]])))

(defn render-step [{:keys [step step-opts max-steps css] :as data}
                   form-fn {:keys [id] :as form} args]
  (let [{:keys [legend]} step-opts
        first-step? (zero? @step)
        last-step? (= (dec max-steps) @step)
        render-navigation (or (get-in form [:options :wizard :render-navigation])
                              render-navigation)]
    [:div {:class (get css :wizard "wizard")
           :key (str "wizard-" id)}
     [:div {:class (get css :legend "legend")} legend]
     [show-form-fn form-fn @step args]
     [render-navigation (assoc data
                               :first-step? first-step?
                               :last-step? last-step?
                               :form form
                               :max-steps max-steps)]]))

(defn run-wizard [form-fn args]
  (let [step (r/atom 0)
        {:keys [form]} args
        default-style-map {:min-height (as-> (get-in form [:options :wizard :steps]) $
                                             (map count $)
                                             (apply max $)
                                             (str (* 10 $) "rem"))}
        max-steps (count (get-in form [:options :wizard :steps]))
        button-element (get-in form [:options :wizard :button/element] elements/button-element)
        button-props (get-in form [:options :wizard :button/props] {})]
    (fn [form-fn args]
      (let [{:keys [params form]} args
            step-opts (get-in form [:options :wizard :steps @step])
            style-map {:style (merge default-style-map
                                     (get-in form [:options :wizard :css])
                                     (get-in params [:wizard :css])
                                     (get-in step-opts [:css]))}]
        (rf/dispatch [:ez-wire.form.wizard/current-step (:id form) @step])
        [render-step {:step step
                      :step-opts step-opts
                      :style-map style-map
                      :max-steps max-steps
                      :button button-element
                      :button-props button-props}
         form-fn form args]))))

(defn wizard
  "Takes a form-fn (as-table, as-list, etc) and puts into a surrounding wizard if the form is to be rendered as a wizard"
  [form-fn]
  (fn [params form & [content]]
    (let [wizard? (helpers/wizard? form)]
      (fn [params form & [content]]
        (let [args {:params params :form form :content content}]
          (if wizard?
            [run-wizard form-fn args]
            (show-form-fn form-fn args)))))))
