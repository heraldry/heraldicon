(ns heraldicon.frontend.ui.element.arms-select
  (:require
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]))

(def list-db-path
  [:arms-list])

(defn- invalidate-arms-cache []
  (state/invalidate-cache list-db-path :all))

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
     :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
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
  (let [[status _arms-list] (state/async-fetch-data list-db-path :all api/fetch-arms-list)]
    (if (= status :done)
      [component
       list-db-path
       on-select
       invalidate-arms-cache
       :selected-arms selected-arms
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
