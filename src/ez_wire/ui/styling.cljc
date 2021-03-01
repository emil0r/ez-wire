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

(defn get-styling [{:keys [style css]} k]
  (let [global-styling (get @styling* k)]
    {:style (merge-style (:style global-styling) style)
     :css (merge-css (:css global-styling) css)}))
