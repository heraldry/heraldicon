(ns heraldry.frontend.ui.element.collection-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.attribution :as attribution]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.tags :as tags]
   [heraldry.frontend.user :as user]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:collection-list])

(defn fetch-collection [collection-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            collection-data (<? (api-request/call :fetch-collection {:id collection-id
                                                                     :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path collection-data]))
        collection-data)
      (catch :default e
        (log/error "fetch collection error:" e)))))

(defn fetch-collection-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-collections-list
             {}
             user-data)
            <?
            :collections))
      (catch :default e
        (log/error "fetch collection list error:" e)))))

(defn fetch-collection-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-collections-for-user
             {:user-id user-id}
             user-data)
            <?
            :collections))
      (catch :default e
        (log/error "fetch collection list by user error:" e)))))

(defn invalidate-collection-cache [user-id]
  (state/invalidate-cache list-db-path user-id))

(defn component [collection-list link-fn refresh-fn & {:keys [hide-ownership-filter?]}]
  (let [user-data (user/data)]
    [filter/legacy-component
     :collection-list
     user-data
     collection-list
     [:name :username :metadata :tags]
     (fn [& {:keys [items]}]
       [:ul.collection-list
        (doall
         (for [collection (sort-by (comp s/lower-case :name) items)]
           (let [username (-> collection :username)]
             ^{:key (:id collection)}
             [:li.collection
              (if (-> collection :is-public)
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
              [tags/tags-view (-> collection :tags keys)]])))])
     refresh-fn
     :hide-ownership-filter? hide-ownership-filter?]))

(defn list-collections [link-to-collection]
  (let [[status collection-list] (state/async-fetch-data
                                  list-db-path
                                  :all
                                  fetch-collection-list)]
    (if (= status :done)
      [component
       collection-list
       link-to-collection
       #(invalidate-collection-cache :all)]
      [:div [tr :string.miscellaneous/loading]])))
