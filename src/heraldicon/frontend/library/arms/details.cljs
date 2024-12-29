(ns heraldicon.frontend.library.arms.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.entity.arms :as entity.arms]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.component.form :as form]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.buttons :as buttons]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.height-limit-mode :as height-limit-mode]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.arms.list :as library.arms.list]
   [heraldicon.frontend.library.arms.shared :refer [entity-type base-context]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.user.session :as session]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [re-frame.core :as rf]))

(rf/reg-sub ::used-charge-variants
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    (entity.arms/charge-variants data)))

(rf/reg-sub ::used-escutcheons
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    (entity.arms/used-escutcheons data)))

(rf/reg-sub ::used-ribbons
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [data [_ _path]]
    (entity.arms/used-ribbons data)))

(defn- charge-attribution [form-db-path]
  [attribution/for-entities @(rf/subscribe [::used-charge-variants (conj form-db-path :data :achievement)])])

(defn- ribbon-attribution [form-db-path]
  [attribution/for-entities @(rf/subscribe [::used-ribbons (conj form-db-path :data :achievement)])])

(defn- escutcheon-attribution [form-db-path]
  [attribution/for-escutcheons
   (interface/render-option :escutcheon base-context)
   @(rf/subscribe [::used-escutcheons (conj form-db-path :data :achievement)])])

(defn- theme-attribution [_form-db-path]
  [attribution/for-theme (interface/render-option :theme base-context)])

(defn- attribution [form-db-path]
  [attribution/attribution {:path form-db-path}
   [charge-attribution form-db-path]
   [ribbon-attribution form-db-path]
   [escutcheon-attribution form-db-path]
   [theme-attribution form-db-path]])

(defn- blazonry [form-db-path]
  [:div.blazonry
   [:h3
    [tr :string.entity/blazon]
    [:span {:style {:font-size "0.75em"}}
     " " [tr :string.miscellaneous/beta-blazon]]]
   [:div.blazon
    (string/tr-raw (interface/blazon (assoc context/default
                                            :path (conj form-db-path :data :achievement :coat-of-arms))) :en)]])

(defn- arms-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-arms])
  (rf/dispatch-sync [::tree/node-select-default
                     ::identifier
                     form-db-path [form-db-path]])
  (layout/three-columns
   [:<>
    [:div {:class (when @(rf/subscribe [::session/height-limit-mode?])
                    "height-limited")}
     [interface/render-component (c/++ base-context :data :achievement)]]
    [:div {:style {:position "absolute"
                   :left "15px"
                   :bottom "15px"
                   :font-size "2em"
                   :display "inline"}}
     [height-limit-mode/selector]]]
   [:<>
    [form/active (c/<< base-context ::tree/identifier ::identifier)]
    [message/display entity-type]
    [buttons/buttons entity-type]
    [blazonry form-db-path]
    [attribution form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [tree/tree
     ::identifier
     [form-db-path
      (conj form-db-path :data :achievement :render-options)
      :spacer
      (conj form-db-path :data :achievement :helms)
      :spacer
      (conj form-db-path :data :achievement :coat-of-arms)
      :spacer
      (conj form-db-path :data :achievement :ornaments)]
     base-context]]
   :banner (let [entity-id @(rf/subscribe [:get (conj form-db-path :id)])
                 entity-version @(rf/subscribe [:get (conj form-db-path :version)])]
             [details/latest-version-banner
              entity-id
              entity-version
              (library.arms.list/on-select {:id entity-id})])))

(defn- load-hdn [hdn-hash]
  (go
    (if hdn-hash
      (try
        (let [s3-hdn-key (str "blazonry-api/hdn/" hdn-hash ".edn")
              hdn-url (if (= (config/get :stage) "prod")
                        (str "https://data.heraldicon.org/" s3-hdn-key)
                        (str "https://local-heraldry-data.s3.amazonaws.com/" s3-hdn-key))
              data (<? (http/fetch hdn-url))]
          data)
        (catch :default _
          default/arms-entity))
      default/arms-entity)))

(defn create-view [{:keys [query-params]}]
  [details/create-view entity-type arms-form #(load-hdn (:base query-params))])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [details/by-id-view (str "arms:" id) version arms-form])
