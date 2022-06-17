(ns heraldicon.entity.user
  (:require
   [heraldicon.config :as config]))

(defn admin? [user-data]
  (-> user-data :username ((config/get :admins))))
