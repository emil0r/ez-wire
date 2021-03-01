(ns ez-wire.ui.table
  (:require [ez-wire.paginator :refer [paginate]]
            [ez-wire.protocols :refer [t]]
            [ez-wire.ui.styling :refer [get-styling]]
            [ez-wire.util :refer [deref? deref-or-value]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn- asc? [order]
  (= order :asc))
(defn- desc? [order]
  (= order :desc))

(defn cell [context {:keys [value props]}]
  [:span props value])

(defn header [{:sort/keys [sort sorts] :as context}
              {:keys [title name props] :as _header}]
  [:span props
   (if (and sorts
            (sorts name))
     [:span
      (cond (and (= (:k @sort) name)
                 (desc? (:order @sort)))
            (:down-arrow context)
            (and (= (:k @sort) name)
                 (asc? (:order @sort)))
            (:up-arrow context)
            :else
            (:square-arrow context))])
   
   (if title
     (t title)
     (t name))])

(defn- assemble-column-options [context
                                {sort-on-click :sort/on-click
                                 :keys [on-click css render] :as column}]
  (assoc column
         :props (merge
                 {:on-click (fn [e]
                              (when sort-on-click
                                (sort-on-click e))
                              (when on-click
                                (on-click e)))}
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

(defn- td-render [context {:keys [key-fn render] :as data}]
  [:td
   {:key (key-fn data)}
   [render context data]])

(defn- get-rows [{:pagination/keys [pagination pp]
                  :keys [sort/sort] :as _context} model]
  (let [model (cond
                pagination (let [{:keys [page]} @pagination]
                             (->> model
                                  (drop (* (dec page) pp))
                                  (take pp)))
                :else model)]
    (if sort
      (let [{:keys [k order]} @sort
            sorted (sort-by k model)]
        (if (asc? order)
          sorted
          (reverse sorted)))
      model)))

(defn- row-render [{:keys [row/key-fn] :as context} row row-options column-ks]
  (let [row (->> column-ks
                 (map #(merge row
                              {:value (% row)}
                              (get row-options %))))]
    [:tr (map #(td-render context %) row)]))


(defn init-context [context]
  (let [context (assoc context :sort/sorts #{})]
    (letfn [(switch-order [{:keys [order]}]
              (if (asc? order)
                :desc
                :asc))
            (add-sorts [context]
              (reduce (fn [out {:keys [sort? name] :as _column}]
                        (if sort?
                          (update out :sort/sorts conj name)
                          out))
                      context (:columns context)))
            (add-default-sort [context]
              (reduce (fn [out {:keys [sort/default? name] :as _column}]
                        (if default?
                          (reduced (assoc context :sort/default name))
                          out))
                      context (:columns context)))
            (add-sort-fn [{:keys [sort/sorts] :as context}]
              (if-not (empty? sorts)
                (let [sort (r/atom {:k (get context :sort/default (first (:sort/sorts context)))
                                    :order :asc})
                      new-columns (reduce (fn [out {:keys [name] :as column}]
                                            (if (sorts name)
                                              (conj out (assoc column :sort/on-click
                                                               (fn [_]
                                                                 (let [data @sort]
                                                                   (if (= (:k data) name)
                                                                     (reset! sort {:k (:k data)
                                                                                   :order (switch-order data)})
                                                                     (reset! sort {:k name
                                                                                   :order :asc}))))))
                                              (conj out column)))
                                          [] (:columns context))]
                  (-> context
                      (assoc :sort/sort sort)
                      (assoc :columns new-columns)))
                context))
            (add-pagination [{:pagination/keys [pp page]
                              :keys [pagination?]
                              :or {pp 50
                                   page 1}
                              :as context}]
              (if pagination?
                (let [length (count (deref-or-value (:model context)))]
                  (assoc context
                         :pagination/pagination (r/atom (paginate
                                                         length
                                                         pp
                                                         page))))
                context))
            (add-possibly-index [{:keys [row/key-fn model] :as context}]
              (if key-fn
                context
                (let [new-model (->> (deref-or-value model)
                                     (map-indexed (fn [idx row]
                                                    (assoc row ::idx idx))))]
                  (if (deref? model)
                    (do (reset! model new-model)
                        context)
                    (assoc context :model new-model)))))
            (fix-names [{:keys [columns] :as context}]
              (assoc context :columns (reduce (fn [out column]
                                                (if-let [f (:fn column)]
                                                  (conj out (-> column
                                                                (assoc :name f)
                                                                (dissoc :fn)))
                                                  (conj out column)))
                                              [] columns)))]
      (-> context
          (fix-names)
          (add-sorts)
          (add-default-sort)
          (add-sort-fn)
          (add-pagination)
          (add-possibly-index))))) 

(defn table [context]
  (r/with-let [;; setup default options that we can send to other functions
               context (merge {:show-columns? true
                               :up-arrow "▲"
                               :down-arrow "▼"
                               :square-arrow "■"
                               :id (random-uuid)}
                              (init-context context))
               {:keys [show-columns? columns]} context
               props (-> context
                         (select-keys [:css :style])
                         (get-styling ::table))
               column-ks (map :name columns)
               columns (->> columns
                            (mapv (comp (juxt :name identity) #(assemble-column-options context %)))
                            (into {}))
               row-options (->> columns
                                (map #(assemble-row-options context %))
                                (into {}))
               model (:model context)]
    [:table props
     [:thead
      (when show-columns?
        [:tr (for [k column-ks]
               (let [{:keys [key-fn] :as column} (get columns k)]
                 ^{:key (key-fn column)}
                 (td-render context column)))])]
     [:tbody
      (let [key-fn (:row/key-fn context)]
        (for [row (get-rows context (deref-or-value model))]
          ^{:key (if key-fn
                   (key-fn row)
                   (::idx row))}
          [row-render context row row-options column-ks]))]]))
