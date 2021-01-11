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
  ([form-fn {:keys [params form-map content]}]
   [form-fn params form-map content])
  ([form-fn field-ks current-step args]
   (let [new-args (-> args
                      (assoc-in [:form-map :field-ks] field-ks)
                      (assoc-in [:form-map :options :wizard :current-step] current-step))]
     (show-form-fn form-fn new-args))))

(defn render-navigation [{:keys [step last-step? first-step? max-steps form-map button css button-props]}]
  (let [data @(rf/subscribe [:ez-wire.form/on-valid (:id form-map)])
        valid-fn (get-in form-map [:options :wizard :valid-fn])
        valid? (helpers/valid? data)]
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
                                 (valid-fn data)
                                 
                                 :else
                                 nil)})
       (if (and last-step? valid?)
         (t :ez-wire.form/done)
         (t ::next))]]]))

(defn render-step [{:keys [step step-opts max-steps css] :as data}
                   form-fn {:keys [id] :as form-map} args]
  (let [{:keys [fields legend]} step-opts
        first-step? (zero? @step)
        last-step? (= (dec max-steps) @step)
        render-navigation (or (get-in form-map [:options :wizard :render-navigation])
                              render-navigation)]
    [:div {:class (get css :wizard "wizard")
           :key (str "wizard-" id)}
     [:div {:class (get css :legend "legend")} legend]
     (show-form-fn form-fn fields @step args)
     (render-navigation (assoc data
                               :first-step? first-step?
                               :last-step? last-step?
                               :form-map form-map
                               :max-steps max-steps))]))

(defn run-wizard [form-fn args]
  (let [step (r/atom 0)
        {:keys [form-map]} args
        default-style-map {:min-height (as-> (get-in form-map [:options :wizard :steps]) $
                                             (map count $)
                                             (apply max $)
                                             (str (* 10 $) "rem"))}
        max-steps (count (get-in form-map [:options :wizard :steps]))
        button-element (get-in form-map [:options :wizard :button/element] elements/button-element)
        button-props (get-in form-map [:options :wizard :button/props] {})]
    (fn [form-fn args]
      (let [{:keys [params form-map]} args
            step-opts (get-in form-map [:options :wizard :steps @step])
            style-map {:style (merge default-style-map
                                     (get-in form-map [:options :wizard :css])
                                     (get-in params [:wizard :css])
                                     (get-in step-opts [:css]))}]
        (rf/dispatch [:ez-wire.form.wizard/current-step (:id form-map) @step])
        (render-step {:step step
                      :step-opts step-opts
                      :style-map style-map
                      :max-steps max-steps
                      :button button-element
                      :button-props button-props}
                     form-fn form-map args)))))

(defn wizard
  "Takes a form-fn (as-table, as-list, etc) and puts into a surrounding wizard if the form is to be rendered as a wizard"
  [form-fn]
  (fn [params form-map & [content]]
    (let [wizard? (= :wizard (get-in form-map [:options :render]))]
      (fn [params form-map & [content]]
        (let [args {:params params :form-map form-map :content content}]
          (if wizard?
            [run-wizard form-fn args]
            (show-form-fn form-fn args)))))))
