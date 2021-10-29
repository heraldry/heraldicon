(ns heraldry.frontend.ui.element.user-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.user :as user]
   [heraldry.strings :as strings]
   [taoensso.timbre :as log]))

(def list-db-path
  [:user-list])

(defn fetch-user-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-users-all
             {}
             user-data)
            <?
            :users))
      (catch :default e
        (log/error "fetch users list error:" e)))))

(defn invalidate-user-cache [key]
  (state/invalidate-cache list-db-path key))

(defn component [user-list link-fn refresh-fn]
  (let [user-data (user/data)]
    [filter/component
     :user-list
     user-data
     user-list
     [:username]
     (fn [& {:keys [items]}]
       [:ul.user-list
        (doall
         (for [user (sort-by (comp s/lower-case :username) items)]
           ^{:key (:id user)}
           [:li.user
            [link-fn user]]))])
     refresh-fn
     :hide-access-filter? true
     :hide-ownership-filter? true]))

(defn list-users [link-to-user]
  (let [[status user-list] (state/async-fetch-data
                            list-db-path
                            :all
                            fetch-user-list)]
    (if (= status :done)
      [component
       user-list
       link-to-user
       #(invalidate-user-cache :all)]
      [:div [tr strings/loading]])))
