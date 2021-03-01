(ns ez-wire.ui.pagination
  "Pagination element designed to use ez-wire.paginator"
  (:require [ez-wire.ui.styling :refer [get-styling]]
            [ez-wire.util :refer [gen-id]]
            [ez-wire.paginator :refer [paginate]]
            [ez-wire.protocols :refer [t]]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(rf/reg-event-db ::paginate (fn [db [_ id page]]
                              (assoc-in db [::pagination id] page)))
(rf/reg-event-db ::cleanup (fn [db [_ id]]
                             (update db ::pagination dissoc id)))
(rf/reg-sub ::pagination (fn [db [_ id]]
                           (get-in db [::pagination id])))



(defn- current? [idx page]
  (= idx page))

(defn- page-item [on-click-fn idx]
  [:li.item {:key idx
             :on-click (on-click-fn idx)} idx])

(defn pagination
  [context]
  (r/with-let [props (-> context
                         (select-keys [:css :style])
                         (get-styling {:css "ez-wire-pagination"} ::pagination))
               pagination (if (:pagination context)
                            (:pagination context)
                            (let [{:keys [length page-size page]
                                   :or {page 1
                                        page-size 20}} context]
                              (assert (number? length) "length has to be a number")
                              (r/atom (paginate length page-size page))))
               {:keys [limit id]
                :or {limit 3
                     id (gen-id)}} context
               on-click-fn (fn [idx] #(do (rf/dispatch [::paginate id idx])
                                          (let [{:keys [page-size length]} @pagination]
                                            (reset! pagination (paginate length page-size idx)))))]
   (let [{:keys [pages page next next-seq prev prev-seq]} @pagination]
     [:div props
      [:ul
       [:li.prev {:class (if (current? 1 page)
                           "current")
                  :on-click (on-click-fn (dec page))}
        (t ::prev)]
       (map #(page-item on-click-fn %) (take limit (reverse prev-seq)))
       [:li.item.current
        page]
       (map #(page-item on-click-fn %) (take limit next-seq))
       [:li.next {:class (if (current? pages page)
                           "current")
                  :on-click (on-click-fn (inc page))}
        (t ::next)]]])
    (finally
      (rf/dispatch [::cleanup id]))))
