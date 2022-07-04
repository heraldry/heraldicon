(ns heraldicon.frontend.element.charge-select
  (:require
   [heraldicon.frontend.blazonry-editor.parser :as blazonry-editor.parser]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [charges-subscription on-select refresh-fn & {:keys [display-selected-item?]
                                                              :as options}]
  [filter/component
   :charge-list
   @(rf/subscribe [::session/data])
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
   (assoc options
          :page-size 20
          :component-styles (if display-selected-item?
                              {:height "75vh"}
                              {:height "90vh"}))])

(defn list-charges [on-select & {:as options}]
  [component
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/charge #(rf/dispatch [::blazonry-editor.parser/update %])])
   on-select
   #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/charge])
   options])
