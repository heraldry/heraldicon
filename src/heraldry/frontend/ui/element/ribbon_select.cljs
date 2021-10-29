(ns heraldry.frontend.ui.element.ribbon-select
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
   [heraldry.strings :as strings]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(def list-db-path
  [:ribbon-list])

(defn fetch-ribbon [ribbon-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            ribbon-data (<? (api-request/call :fetch-ribbon {:id ribbon-id
                                                             :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path ribbon-data]))
        ribbon-data)
      (catch :default e
        (log/error "fetch ribbon error:" e)))))

(defn fetch-ribbon-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-ribbons
             {}
             user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbon list error:" e)))))

(defn fetch-ribbon-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-ribbon-for-user
             {:user-id user-id}
             user-data)
            <?
            :ribbons))
      (catch :default e
        (log/error "fetch ribbon list by user error:" e)))))

(defn invalidate-ribbon-cache [key]
  (state/invalidate-cache list-db-path key))

(defn component [ribbon-list link-fn refresh-fn & {:keys [hide-ownership-filter?]}]
  (let [user-data (user/data)]
    [filter/component
     :ribbon-list
     user-data
     ribbon-list
     [:name :username :tags]
     (fn [& {:keys [items]}]
       [:ul.ribbon-list
        (doall
         (for [ribbon (sort-by (comp s/lower-case :name) items)]
           (let [username (-> ribbon :username)]
             ^{:key (:id ribbon)}
             [:li.ribbon
              (if (-> ribbon :is-public)
                [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]])
              " "
              [link-fn ribbon]
              [tr strings/by]
              [:a {:href (attribution/full-url-for-username username)
                   :target "_blank"} username]
              " "
              [tags/tags-view (-> ribbon :tags keys)]])))])
     refresh-fn
     :hide-ownership-filter? hide-ownership-filter?]))

(defn list-ribbon [link-to-ribbon]
  (let [[status ribbon-list] (state/async-fetch-data
                              list-db-path
                              :all
                              fetch-ribbon-list)]
    (if (= status :done)
      [component
       ribbon-list
       link-to-ribbon
       #(invalidate-ribbon-cache :all)]
      [:div [tr strings/loading]])))
