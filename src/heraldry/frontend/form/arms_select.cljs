(ns heraldry.frontend.form.arms-select
  (:require [cljs.core.async :refer [go]]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.filter :as filter]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.tags :as tags]
            [heraldry.frontend.user :as user]
            [heraldry.attribution :as attribution]
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
             :fetch-arms-all
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

(defn component [arms-list link-fn refresh-fn & {:keys [hide-ownership-filter?]}]
  (let [user-data (user/data)]
    [filter/component
     :arms-list
     user-data
     arms-list
     [:name :username :tags]
     (fn [& {:keys [items]}]
       [:ul.arms-list
        (doall
         (for [arms (sort-by (comp s/lower-case :name) items)]
           (let [username (-> arms :username)]
             ^{:key (:id arms)}
             [:li.arms
              (if (-> arms :is-public)
                [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
                [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]])
              " "
              [link-fn arms]
              " by "
              [:a {:href (attribution/full-url-for-username username)
                   :target "_blank"} username]
              " "
              [tags/tags-view (-> arms :tags keys)]])))])
     refresh-fn
     :hide-ownership-filter? hide-ownership-filter?]))

(defn list-arms [link-to-arms]
  (let [[status arms-list] (state/async-fetch-data
                            list-db-path
                            :all
                            fetch-arms-list)]
    (if (= status :done)
      [component
       arms-list
       link-to-arms
       #(invalidate-arms-cache :all)]
      [:div "loading..."])))
