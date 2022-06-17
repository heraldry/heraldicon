(ns heraldicon.frontend.maintenance
  (:require
   [heraldicon.config :as config]
   [heraldicon.entity.user :as entity.user]
   [heraldicon.frontend.user :as user]))

(defn view [_m]
  [:div {:style {:padding-left "10px"}}
   [:h2 "Maintenance mode"]
   [:p "Sorry about that, we should be back online soon."]])

(def ^:private affected-section?
  #{"arms"
    "collection"
    "ribbon"
    "charge"
    "user"
    "account"})

(defn in-effect? [section]
  (and (config/get :maintenance-mode?)
       (not (entity.user/admin? (user/data)))
       (affected-section? section)))
