(ns heraldry.frontend.ui.element.ribbon-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.preview :as preview]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:ribbon-list])

(defn fetch-ribbon [ribbon-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            ribbon-data (<? (api-request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path ribbon-data]))
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon error:" e)))))

(defn fetch-ribbon-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-ribbons-list
             {}
             user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbon list error:" e)))))

(defn fetch-ribbon-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-ribbon-for-user
             {:user-id user-id}
             user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbon list by user error:" e)))))

(defn invalidate-ribbons-cache []
  (let [user-data (user/data)
        user-id (:user-id user-data)]
    (rf/dispatch-sync [:set list-db-path nil])
    (state/invalidate-cache list-db-path user-id)
    (state/invalidate-cache [:all-ribbons] :all-ribbons)))

(defn component [ribbon-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                 selected-ribbon
                                                                 display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :ribbon-list
     user-data
     ribbon-list-path
     [:name :username :metadata :tags]
     :ribbon
     on-select
     refresh-fn
     :sort-fn (juxt (comp s/lower-case :name)
                    :type
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles {:height "calc(80vh - 3em)"}
     :selected-item selected-ribbon
     :display-selected-item? display-selected-item?]))

(defn list-ribbons [on-select & {:keys [selected-ribbon
                                        display-selected-item?]}]
  (let [[status _ribbon-list] (state/async-fetch-data
                               list-db-path
                               :all
                               fetch-ribbon-list)]
    (if (= status :done)
      [component
       list-db-path
       on-select
       invalidate-ribbons-cache
       :selected-ribbon selected-ribbon
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
