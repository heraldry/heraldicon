(ns heraldry.frontend.ui.element.arms-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:arms-list])

(defn fetch-arms [arms-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            arms-data (<? (api-request/call :fetch-arms {:id arms-id
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
        (-> (api-request/call
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
        (-> (api-request/call
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
     :sort-fn (juxt (comp s/lower-case :name)
                    :type
                    :id
                    :version)
     :page-size 10
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles {:height "calc(80vh - 3em)"}
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
