(ns heraldicon.frontend.ui.element.collection-select
  (:require
   [clojure.string :as s]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.loading :as loading]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.tags :as tags]
   [heraldicon.frontend.user :as user]))

(def list-db-path
  [:collection-list])

(defn- invalidate-collections-cache []
  (state/invalidate-cache list-db-path :all))

(defn component [collection-list link-fn refresh-fn & {:keys [hide-ownership-filter?]}]
  (let [user-data (user/data)]
    [filter/legacy-component
     :collection-list
     user-data
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
     :hide-ownership-filter? hide-ownership-filter?]))

(defn list-collections [link-to-collection]
  (let [[status collection-list] (state/async-fetch-data list-db-path :all api/fetch-collections-list)]
    (if (= status :done)
      [component collection-list link-to-collection invalidate-collections-cache]
      [loading/loading])))
