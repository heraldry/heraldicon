(ns heraldicon.frontend.ui.element.user-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
   [taoensso.timbre :as log]))

(def ^:private list-db-path
  [:user-list])

(defn fetch-user [username]
  (go
    (try
      (<? (api.request/call :fetch-user {:username username} (user/data)))
      (catch :default e
        (log/error "fetch user error:" e)))))

(defn- fetch-user-list []
  (go
    (try
      (-> (api.request/call :fetch-users-all {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch users list error:" e)))))

(defn- invalidate-user-cache [key]
  (state/invalidate-cache list-db-path key))

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
  (let [[status user-list] (state/async-fetch-data list-db-path :all fetch-user-list)]
    (if (= status :done)
      [component user-list link-to-user #(invalidate-user-cache :all)]
      [:div [tr :string.miscellaneous/loading]])))
