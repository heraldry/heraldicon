(ns heraldicon.frontend.ui.element.collection-select
  (:require
   [clojure.string :as s]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.repository.entity-list :as entity-list]
   [heraldicon.frontend.status :as status]
   [heraldicon.frontend.ui.element.tags :as tags]
   [heraldicon.frontend.user.session :as session]
   [re-frame.core :as rf]))

(defn component [collection-list link-fn refresh-fn & {:keys [hide-ownership-filter?
                                                              predicate-fn]}]
  (let [session @(rf/subscribe [::session/data])]
    [filter/legacy-component
     :collection-list
     session
     collection-list
     [:name :username :metadata :tags]
     (fn [& {:keys [items]}]
       (into [:ul.collection-list]
             (map (fn [collection]
                    (let [username (:username collection)]
                      ^{:key (:id collection)}
                      [:li.collection
                       (if (-> :access collection (= :public))
                         [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                         [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]])
                       " "
                       [link-fn collection]
                       " "
                       [tr :string.miscellaneous/by]
                       " "
                       [:a {:href (attribution/full-url-for-username username)
                            :target "_blank"} username]

                       " "
                       [tags/tags-view (-> collection :tags keys)]])))
             (sort-by (comp s/lower-case :name) items)))
     refresh-fn
     :hide-ownership-filter? hide-ownership-filter?
     :predicate-fn predicate-fn]))

(defn list-collections [link-to-collection]
  (status/default
   (rf/subscribe [::entity-list/data :heraldicon.entity.type/collection])
   (fn [{:keys [entities]}]
     [component
      entities
      link-to-collection
      #(rf/dispatch [::entity-list/clear :heraldicon.entity.type/collection])])))
