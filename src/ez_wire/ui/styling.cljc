(ns ez-wire.ui.styling)


(def styling* (atom {}))

(defmacro with-styling [styling & body]
  `(do (reset! styling* ~styling)
       ~@body))

(defn- merge-style [style1 style2]
  (merge style1 style2))

(defn- merge-css [css1 css2]
  (cond (and (string? css1)
             (string? css2))
        [css1 css2]
        (and (string? css1)
             (vector? css2))
        (conj css2 css1)
        (and (vector? css1)
             (string? css2))
        (conj css1 css2)
        :else
        (into css1 css2)))

(defn get-styling
  ([properties k]
   (get-styling properties nil k))
  ([{:keys [style css] :as properties} default k]
   (let [global-styling (get @styling* k)]
     (merge (dissoc properties :style :css)
            {:style (-> (:style default)
                        (merge-style (:style global-styling))
                        (merge-style style))
             :class (-> (:css default)
                        (merge-css (:css global-styling))
                        (merge-css css))}))))
