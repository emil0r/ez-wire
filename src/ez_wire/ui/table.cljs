(ns ez-wire.ui.table
  (:require [ez-wire.protocols :refer [t]]
            [ez-wire.util :refer [deref-or-value]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn cell [{:keys [value props]}]
  [:span props value])

(defn header [{:keys [title name props] :as header}]
  [:span props
   (if title
     (t title)
     (t name))])

(defn- assemble-column-options [context {:keys [on-click css render] :as column}]
  (assoc column
         :props (merge
                 (if on-click
                   {:on-click on-click})
                 (if css
                   {:class css}))
         :key-fn (or (:column/key-fn context)
                     (:column/key-fn column)
                     :name)
         :render (if render
                   render
                   header)))

(defn- remove-keyword-namespace [k]
  (keyword (name k)))

(defn- assemble-row-options [context [k column]]
  [k (->> (merge (select-keys context [:row/on-click :row/key-fn :row/render])
                 (select-keys column [:row/on-click :row/key-fn :row/render])
                 {:row/render cell
                  :row/key-fn (:name column)})
          (map (fn [[k v]]
                 [(remove-keyword-namespace k) v]))
          (into {}))])

(defn- td-render [{:keys [key-fn render] :as data}]
  [:td
   {:key (key-fn data)}
   [render data]])

(defn- get-rows [sorter model]
  model)

(defn- row-render [row row-options column-ks]
  [:tr (->> column-ks
            (map #(merge row
                         {:value (get row %)}
                         (get row-options %)))
            (map td-render))])

(defn init-context [{:keys [columns] :as context}]
  (let [context (assoc context
                       :sorts #{})]
   (letfn [(add-sorts [context]
             (reduce (fn [out {:keys [sort? name] :as _column}]
                       (if sort?
                         (update out :sorts conj name)
                         out))
                     context (:columns context)))
           (add-default-sort [context]
             (reduce (fn [out {:keys [sort/default? name] :as _column}]
                       (if default?
                         (reduced (assoc context :sort/default name))
                         out))
                     context (:columns context)))
           (add-sort-fn [context]
             ;; TODO: FIX THIS. ADD ONCLICK FUNCTION THAT TRIGGERS THE RATOM
             (if-not (empty? (:sorts context))
               (let [sort (r/atom (:sort/default context))
                     ]
                 context)
               context))]
     (-> context
         (add-sorts)
         (add-default-sort)
         (add-sort-fn))))) 

(defn table [{:keys [columns model css] :as context}]
  (r/with-let [;; setup default options that we can send to other functions
               context (merge {:show-columns? true
                               :up-arrow "▲"
                               :down-arrow "▼"}
                              (init-context context))
               {:keys [show-columns?]} context
               props (if css
                       {:class css})
               column-ks (map :name columns)
               columns (->> columns
                            (mapv (comp (juxt :name identity) #(assemble-column-options context %)))
                            (into {}))
               row-options (->> columns
                                (map #(assemble-row-options context %))
                                (into {}))]
    [:table props
     [:thead
      (when show-columns?
        [:tr (for [k column-ks]
               (let [{:keys [key-fn] :as column} (get columns k)]
                 ^{:key (key-fn column)}
                 (td-render column)))])]
     [:tbody
      (for [row (get-rows context (deref-or-value model))]
        (row-render row row-options column-ks))]]))
