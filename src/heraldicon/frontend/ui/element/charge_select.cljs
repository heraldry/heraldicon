(ns heraldicon.frontend.ui.element.charge-select
  (:require
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.ui.element.blazonry-editor :as blazonry-editor]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [charges-subscription on-select refresh-fn & {:keys [hide-ownership-filter?
                                                                     selected-charge
                                                                     display-selected-item?
                                                                     predicate-fn]}]
  (let [session @(rf/subscribe [::session/data])]
    [filter/component
     :charge-list
     session
     charges-subscription
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
     :display-selected-item? display-selected-item?
     :predicate-fn predicate-fn]))

(defn list-charges [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/charge blazonry-editor/update-parser])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/charge])
   options])
