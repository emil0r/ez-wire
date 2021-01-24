(ns ez-wire.admin.helpers
  (:require [re-frame.core :as rf]))

(defn navigate
  ([]
   (rf/dispatch [:ez-wire.admin/navigation {:module :ez-wire.admin/all-modules
                                            :entity nil
                                            :entity-id nil}]))
  ([module]
   (rf/dispatch [:ez-wire.admin/navigation {:module module
                                            :entity nil
                                            :entity-id nil}]))
  ([module entity]
   (rf/dispatch [:ez-wire.admin/navigation {:module module
                                            :entity entity
                                            :entity-id nil}]))
  ([module entity entity-id]
   (rf/dispatch [:ez-wire.admin/navigation {:module module
                                            :entity entity
                                            :entity-id entity-id}])))
(defn home? [module]
  (= module :ez-wire.admin/all-modules))

(def home :ez-wire.admin/all-modules)
