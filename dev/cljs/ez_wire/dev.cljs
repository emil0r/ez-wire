(ns ^:figwheel-no-load ez-wire.dev
  (:require
    [ez-wire.test-page :as test-page]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(test-page/init!)
