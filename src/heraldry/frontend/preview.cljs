(ns heraldry.frontend.preview
  (:require
   [clojure.string :as s]
   [heraldry.attribution :as attribution]
   [heraldry.config :as config]
   [heraldry.frontend.ui.element.tags :as tags]
   [heraldry.frontend.user :as user]
   [heraldry.util :as util]
   [re-frame.core :as rf]))

(defn effective-version [data]
  (let [version (:version data)]
    (if (zero? version)
      (or (:latest-version data) 0)
      version)))

(defn preview-url [kind {:keys [id] :as arms} & {:keys [width height]}]
  (let [url (or (config/get :heraldry-site-url)
                (config/get :heraldry-url))]
    (str url "/preview/" (name kind) "/" (-> id (s/split #":") last) "/" (effective-version arms) "/preview.png"
         (when (and width height)
           (str "?width=" width "&height=" height)))))

(defn preview-image [url]
  (let [loaded-flag-path [:ui :preview-image-loaded? url]
        loaded? @(rf/subscribe [:get loaded-flag-path])]
    [:<>
     [:img {:src url
            :on-load (when-not loaded? #(rf/dispatch [:set loaded-flag-path true]))
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

(defn arms [arms link-fn & {:keys [selected?]}]
  (let [username (-> arms :username)
        link-args (get (link-fn arms) 1)
        own-username (:username (user/data))]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when selected? "selected")}
      [:div.filter-result-card-header
       [:div.filter-result-card-owner
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"
             :title username}
         [:img {:src (util/avatar-url username)
                :style {:border-radius "50%"}}]]]
       [:div.filter-result-card-title
        (:name arms)]
       [:div.filter-result-card-access
        (when (= own-username username)
          (if (-> arms :is-public)
            [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
            [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))]]
      [:a.filter-result-card-preview link-args
       [preview-image (preview-url :arms arms :width 300 :height 220)]]
      [:div.filter-result-card-tags
       [tags/tags-view (-> arms :tags keys)]]]]))

(defn ribbon [ribbon link-fn & {:keys [selected?]}]
  (let [username (-> ribbon :username)
        link-args (get (link-fn ribbon) 1)
        own-username (:username (user/data))]
    [:li.filter-result-card-wrapper
     [:div.filter-result-card {:class (when selected? "selected")}
      [:div.filter-result-card-header
       [:div.filter-result-card-owner
        [:a {:href (attribution/full-url-for-username username)
             :target "_blank"
             :title username}
         [:img {:src (util/avatar-url username)
                :style {:border-radius "50%"}}]]]
       [:div.filter-result-card-title
        (:name ribbon)]
       [:div.filter-result-card-access
        (when (= own-username username)
          (if (-> ribbon :is-public)
            [:div.tag.public {:style {:width "0.9em"}} [:i.fas.fa-lock-open]]
            [:div.tag.private {:style {:width "0.9em"}} [:i.fas.fa-lock]]))]]
      [:a.filter-result-card-preview link-args
       [preview-image (preview-url :ribbon ribbon :width 300 :height 220)]]
      [:div.filter-result-card-tags
       [tags/tags-view (-> ribbon :tags keys)]]]]))
