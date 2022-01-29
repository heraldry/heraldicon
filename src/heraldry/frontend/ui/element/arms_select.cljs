(ns heraldry.frontend.ui.element.arms-select
  (:require
   [cljs.core.async :refer [go]]
   [clojure.string :as s]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.attribution :as attribution]
   [heraldry.config :as config]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.filter :as filter]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.element.tags :as tags]
   [heraldry.frontend.user :as user]
   [heraldry.util :as util]
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

(defn effective-version [data]
  (let [version (:version data)]
    (if (zero? version)
      (:latest-version data)
      version)))

(defn arms-preview-url [{:keys [id] :as arms} & {:keys [width height]}]
  (let [url (or (config/get :heraldry-site-url)
                (config/get :heraldry-url))]
    (str url "/preview/arms/" (-> id (s/split #":") last) "/" (effective-version arms) "/preview.png"
         (when (and width height)
           (str "?width=" width "&height=" height)))))

(defn preview-image [url]
  (let [loaded-flag-path [:ui :preview-image-loaded? url]
        loaded? @(rf/subscribe [:get loaded-flag-path])]
    [:<>
     [:img {:src url
            :on-load #(rf/dispatch [:set loaded-flag-path true])
            :style {:display (if loaded? "block" "none")
                    :position "absolute"
                    :margin "auto"
                    :top 0
                    :left 0
                    :right 0
                    :bottom 0
                    :max-width "100%"
                    :max-height "100%"}}]
     (when-not loaded?
       [:div.loader {:style {:font-size "0.5em"
                             :position "absolute"
                             :margin "auto"
                             :top 0
                             :left 0
                             :right 0
                             :bottom 0}}])]))

(defn preview-arms [arms link-fn & {:keys [selected?]}]
  (let [username (-> arms :username)
        link-args (get (link-fn arms) 1)
        own-username (:username (user/data))]
    [:li.arms-card-wrapper
     [:div.arms-card {:class (when selected? "selected")}
      [:div.arms-card-header
       [:div.arms-card-owner
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"
             :title username}
         [:img {:src (util/avatar-url username)
                :style {:border-radius "50%"}}]]]
       [:div.arms-card-title
        (:name arms)]
       [:div.arms-card-access
        (when (= own-username username)
          (if (-> arms :is-public)
            [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
            [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))]]
      [:a.arms-card-preview link-args
       [preview-image (arms-preview-url arms :width 300 :height 220)]]
      [:div.arms-card-tags
       [tags/tags-view (-> arms :tags keys)]]]]))

(defn component [arms-list link-fn refresh-fn & {:keys [hide-ownership-filter?
                                                        selected-arms]}]
  (let [user-data (user/data)]
    [filter/component
     :arms-list
     user-data
     arms-list
     [:name :username :metadata :tags]
     (fn [& {:keys [items]}]
       [:ul.arms-list
        (doall
         (for [arms items]
           ^{:key (:id arms)}
           [preview-arms arms link-fn :selected? (filter/selected-item? selected-arms arms)]))])
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
