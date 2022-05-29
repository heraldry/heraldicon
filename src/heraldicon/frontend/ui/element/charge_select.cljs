(ns heraldicon.frontend.ui.element.charge-select
  (:require
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(def list-db-path
  [:charge-list])

(defn invalidate-charges-cache []
  (state/invalidate-cache list-db-path :all))

(defn component [charge-list-path on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                 selected-charge
                                                                 display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :charge-list
     user-data
     charge-list-path
     [:name :username :metadata :tags
      [:data :charge-type] [:data :attitude] [:data :facing] [:data :attributes] [:data :colours]]
     :charge
     on-select
     refresh-fn
     :sort-fn (juxt (comp filter/normalize-string-for-sort :name)
                    #(-> % :data :charge-type)
                    :id
                    :version)
     :page-size 20
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles (if display-selected-item?
                         {:height "75vh"}
                         {:height "90vh"})
     :selected-item selected-charge
     :display-selected-item? display-selected-item?]))

(defn list-charges [on-select & {:keys [selected-charge
                                        display-selected-item?]}]
  (let [[status _charges-list] (state/async-fetch-data
                                list-db-path :all api/fetch-charges-list
                                :on-success #(rf/dispatch
                                              [:heraldicon.frontend.ui.element.blazonry-editor/update-parser %]))]
    (if (= status :done)
      [component
       list-db-path
       on-select
       invalidate-charges-cache
       :selected-charge selected-charge
       :display-selected-item? display-selected-item?]
      [:div [tr :string.miscellaneous/loading]])))
