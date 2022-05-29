(ns heraldicon.frontend.ui.element.ribbon-select
  (:require
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]))

(def ^:private list-db-path
  [:ribbon-list])

(defn invalidate-ribbons-cache []
  (state/invalidate-cache list-db-path :all))

(defn- component [ribbon-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
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
     :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
                    :type
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles (if display-selected-item?
                         {:height "80vh"}
                         {:height "90vh"})
     :selected-item selected-ribbon
     :display-selected-item? display-selected-item?]))

(defn list-ribbons [on-select & {:keys [selected-ribbon
                                        display-selected-item?]}]
  (let [[status _ribbon-list] (state/async-fetch-data list-db-path :all api/fetch-ribbons-list)]
    (if (= status :done)
      [component list-db-path on-select invalidate-ribbons-cache
       :selected-ribbon selected-ribbon
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
