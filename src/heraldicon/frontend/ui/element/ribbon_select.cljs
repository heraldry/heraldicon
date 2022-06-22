(ns heraldicon.frontend.ui.element.ribbon-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user :as user]
   [re-frame.core :as rf]))

(defn- component [ribbons-subscription on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                      selected-ribbon
                                                                      display-selected-item?]}]
  (let [user-data (user/data)]
    [filter/component
     :ribbon-list
     user-data
     ribbons-subscription
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
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/ribbon])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/ribbon])
   :selected-ribbon selected-ribbon
   :display-selected-item? display-selected-item?])
