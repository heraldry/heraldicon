(ns heraldicon.entity.user
  (:require
   [heraldicon.config :as config]))

(defn admin? [user]
  (-> user :username ((config/get :admins))))
