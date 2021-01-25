(ns ez-wire.ui.table
  (:require [ez-wire.protocols :refer [t]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn cell [{:keys [value props]}]
  [:span props value])

(defn header [{:keys [title name props]}]
  [:span props
   (if title
     (t title)
     (t name))])

(defn- assemble-column-options [{:keys [on-click css render] :as column}]
  (assoc column
         :props (merge
                 (if on-click
                   {:on-click on-click})
                 (if css
                   {:class css}))
         :render (if render
                   render
                   header)))

(defn- remove-keyword-namespace [k]
  (keyword (name k)))

(defn- assemble-row-options [context [k column]]
  [k (->> (merge (select-keys context [:row/on-click :row/key :row/render])
                 (select-keys column [:row/on-click :row/key :row/render])
                 {:row/render cell})
          (map (fn [[k v]]
                 [(remove-keyword-namespace k) v]))
          (into {}))])

(defn- td-render [{:keys [render] :as value}]
  [:td [render value]])

(defn- get-rows [model]
  model)

(defn- row-render [row row-options column-ks]
  [:tr (->> column-ks
            (map #(merge {:value (get row %)}
                         (get row-options %)))
            (map td-render))])

(defn table [{:keys [columns model css
                     show-columns?]
              :or {show-columns? true}
              :as context}]
  (r/with-let [props (if css
                       {:class css})
               column-ks (map :name columns)
               columns (->> columns
                            (mapv (comp (juxt :name identity) assemble-column-options))
                            (into {}))
               row-options (->> columns
                                (map #(assemble-row-options context %))
                                (into {}))]
    [:table props
     [:thead
      (when show-columns?
        [:tr (map #(td-render (get columns %)) column-ks)])]
     [:tbody
      (let [rows (get-rows model)]
        (map #(row-render % row-options column-ks) rows))]]))
