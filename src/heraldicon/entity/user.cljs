(ns heraldicon.entity.user
  (:require
   [heraldicon.config :as config]))

(defn admin? [session]
  (-> session :username ((config/get :admins))))
