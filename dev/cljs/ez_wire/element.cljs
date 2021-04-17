(ns ez-wire.element
  (:require [cljsjs.semantic-ui-react]
            [goog.object]))

;; Easy handle to the top-level extern for semantic-ui-react
(def semantic-ui js/semanticUIReact)

(defn component
  "Get a component from sematic-ui-react:
    (component \"Button\")
    (component \"Menu\" \"Item\")"
  [k & ks]
  (if (seq ks)
    (apply goog.object/getValueByKeys semantic-ui k ks)
    (goog.object/get semantic-ui k)))

(def container      (component "Container"))
(def button         (component "Button"))
(def segment        (component "Segment"))
(def dimmer         (component "Dimmer"))
(def loader         (component "Loader"))
(def message        (component "Message"))
(def message-header (component "Message" "Header"))
(def input          (component "Input"))


(defn dropdown [{:keys [model options]}]
  [:select {:value @model
            :on-change #(let [value (-> % .-target .-value)]
                          (reset! model value))}
   (for [option options]
     [:option option])])

(defn text [{:keys [model]}]
  [:div @model])
