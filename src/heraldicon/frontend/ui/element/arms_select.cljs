(ns heraldicon.frontend.ui.element.arms-select
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
   [heraldicon.util :as util]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:arms-list])

(defn fetch-arms [arms-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            arms-data (<? (api.request/call :fetch-arms {:id arms-id
                                                         :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path arms-data]))
        arms-data)
      (catch :default e
        (log/error "fetch arms error:" e)))))

(defn fetch-arms-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api.request/call
             :fetch-arms-list
             {}
             user-data)
            <?
            :arms))
      (catch :default e
        (log/error "fetch arms list error:" e)))))

(defn fetch-arms-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api.request/call
             :fetch-arms-for-user
             {:user-id user-id}
             user-data)
            <?
            :arms))
      (catch :default e
        (log/error "fetch arms list by user error:" e)))))

(defn invalidate-arms-cache [key]
  (state/invalidate-cache list-db-path key))

(defn component [arms-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                               selected-arms
                                                               display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :arms-list
     user-data
     arms-list-path
     [:name :username :metadata :tags]
     :arms
     on-select
     refresh-fn
     :sort-fn (juxt (comp util/normalize-string-for-sort :name)
                    :type
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles (if display-selected-item?
                         {:height "80vh"}
                         {:height "90vh"})
     :selected-item selected-arms
     :display-selected-item? display-selected-item?]))

(defn list-arms [on-select & {:keys [selected-arms
                                     display-selected-item?]}]
  (let [[status _arms-list] (state/async-fetch-data
                             list-db-path
                             :all
                             fetch-arms-list)]
    (if (= status :done)
      [component
       list-db-path
       on-select
       #(invalidate-arms-cache :all)
       :selected-arms selected-arms
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
