(ns heraldicon.frontend.ui.element.collection-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.filter :as filter]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.tags :as tags]
   [heraldicon.frontend.user :as user]
   [taoensso.timbre :as log]))

(def list-db-path
  [:collection-list])

(defn- fetch-collection-list []
  (go
    (try
      (-> (api.request/call :fetch-collections-list {} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch collection list error:" e)))))

(defn fetch-collection-list-by-user [user-id]
  (go
    (try
      (-> (api.request/call :fetch-collections-for-user {:user-id user-id} (user/data))
          <?
          :items)
      (catch :default e
        (log/error "fetch collection list by user error:" e)))))

(defn- invalidate-collection-cache [user-id]
  (state/invalidate-cache list-db-path user-id))

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
  (let [[status collection-list] (state/async-fetch-data list-db-path :all fetch-collection-list)]
    (if (= status :done)
      [component collection-list link-to-collection #(invalidate-collection-cache :all)]
      [:div [tr :string.miscellaneous/loading]])))
