(ns ez-wire.test-admin
  (:require [ez-wire.admin :as admin]))


(defn init! []
  (admin/register-module {:module/key :animals
                          :module/name "Animals"}))

(defn admin-page []
  (init!)
  [admin/admin-index])
