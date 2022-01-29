(ns heraldry.frontend.ui.element.ribbon-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.preview :as preview]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.user :as user]
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
             :fetch-ribbons-list
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


(defn component [ribbon-list link-fn refresh-fn & {:keys [hide-ownership-filter?
                                                          selected-ribbon]}]
  (let [user-data (user/data)]
    [filter/component
     :ribbon-list
     user-data
     ribbon-list
     [:name :username :tags]
     (fn [& {:keys [items]}]
       [:ul.filter-results
        (doall
         (for [ribbon items]
           ^{:key (:id ribbon)}
           [preview/ribbon ribbon link-fn :selected? (filter/selected-item? selected-ribbon ribbon)]))])
     refresh-fn
     :sort-fn (fn [ribbon]
                [(not (and selected-ribbon
                           (filter/selected-item? selected-ribbon ribbon)))
                 (-> ribbon :name s/lower-case)])
     :page-size 10
     :hide-ownership-filter? hide-ownership-filter?
     :selected-item selected-ribbon]))

(defn list-ribbons [link-to-ribbon & {:keys [selected-ribbon]}]
  (let [[status ribbon-list] (state/async-fetch-data
                              list-db-path
                              :all
                              fetch-ribbon-list)]
    (if (= status :done)
      [component
       ribbon-list
       link-to-ribbon
       #(invalidate-ribbon-cache :all)
       :selected-ribbon selected-ribbon]
      [:div [tr :string.miscellaneous/loading]])))
