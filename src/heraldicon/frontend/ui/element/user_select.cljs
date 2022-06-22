(ns heraldicon.frontend.ui.element.user-select
  (:require
   [clojure.string :as s]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.user-list :as repository.user-list]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn- component [user-list link-fn refresh-fn]
  (let [user-data (user/data)]
    [filter/legacy-component
     :user-list
     user-data
     user-list
     [:username]
     (fn [& {:keys [items]}]
       (into [:ul.user-list]
             (map (fn [user]
                    ^{:key (:id user)}
                    [:li.user
                     [link-fn user]]))
             (sort-by (comp s/lower-case :username) items)))
     refresh-fn
     :hide-access-filter? true
     :hide-ownership-filter? true]))

(defn list-users [link-to-user]
  (let [{:keys [status users]} @(rf/subscribe [::repository.user-list/data])]
    (if (= status :done)
      [component users link-to-user #(rf/dispatch [::repository.user-list/clear])]
      [status/loading])))
