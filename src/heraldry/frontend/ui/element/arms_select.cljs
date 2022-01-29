(ns heraldry.frontend.ui.element.arms-select
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
  [:arms-list])

(defn fetch-arms [arms-id version target-path]
  (go
    (try
      (let [user-data (user/data)
            arms-data (<? (api-request/call :fetch-arms {:id arms-id
                                                         :version version} user-data))]
        (when target-path
          (rf/dispatch [:set target-path arms-data]))
        arms-data)
      (catch :default e
        (log/error "fetch arms error:" e)))))

(defn fetch-arms-list []
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-arms-list
             {}
             user-data)
            <?
            :arms))
      (catch :default e
        (log/error "fetch arms list error:" e)))))

(defn fetch-arms-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-arms-for-user
             {:user-id user-id}
             user-data)
            <?
            :arms))
      (catch :default e
        (log/error "fetch arms list by user error:" e)))))

(defn invalidate-arms-cache [key]
  (state/invalidate-cache list-db-path key))

(defn component [arms-list link-fn refresh-fn & {:keys [hide-ownership-filter?
                                                        selected-arms]}]
  (let [user-data (user/data)]
    [filter/component
     :arms-list
     user-data
     arms-list
     [:name :username :metadata :tags]
     (fn [& {:keys [items]}]
       [:ul.filter-results
        (doall
         (for [arms items]
           ^{:key (:id arms)}
           [preview/arms arms link-fn :selected? (filter/selected-item? selected-arms arms)]))])
     refresh-fn
     :sort-fn (fn [arms]
                [(not (and selected-arms
                           (filter/selected-item? selected-arms arms)))
                 (-> arms :name s/lower-case)])
     :page-size 10
     :hide-ownership-filter? hide-ownership-filter?
     :component-styles {:height "calc(80vh - 3em)"}
     :selected-item selected-arms]))

(defn list-arms [link-to-arms & {:keys [selected-arms]}]
  (let [[status arms-list] (state/async-fetch-data
                            list-db-path
                            :all
                            fetch-arms-list)]
    (if (= status :done)
      [component
       arms-list
       link-to-arms
       #(invalidate-arms-cache :all)
       :selected-arms selected-arms]
      [:div [tr :string.miscellaneous/loading]])))
