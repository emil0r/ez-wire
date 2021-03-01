(ns ez-wire.test-styling
  (:require [cljs.test :refer-macros [is are deftest testing]]
            [ez-wire.ui.styling :refer [get-styling] :refer-macros [with-styling]]))


(def style1 {::test {:css ["foo" "bar"]
                     :style {:margin-top "20px"}}})

(def style2 {::test {:css ["fox" "bear"]
                     :style {:margin-bottom "20px"}}})

(deftest styling
  (testing "with-style"
    (is (= ["foo" "bar"]
           (with-styling style1
             (:css (get-styling {} ::test)))))))
