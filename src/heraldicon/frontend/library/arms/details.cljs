(ns heraldicon.frontend.library.arms.details
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.charge :as charge]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.buttons :as buttons]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.arms.shared :refer [entity-type base-context]]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.ribbon :as ribbon]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.render.achievement :as achievement]
   [re-frame.core :as rf]))

(defn- charge-attribution [form-db-path]
  (let [used-charges @(rf/subscribe [:used-charge-variants (conj form-db-path :data :achievement)])
        charges-data (map charge/fetch-charge-data used-charges)]
    (when (-> charges-data first :id)
      [:<>
       [:h3 [tr :string.entity/charges]]
       (into [:ul]
             (keep (fn [charge]
                     (when (:id charge)
                       ^{:key charge}
                       [attribution/for-charge
                        {:path [:context :charge-data]
                         :charge-data charge}])))
             charges-data)])))

(defn- ribbon-attribution [form-db-path]
  (let [used-ribbons @(rf/subscribe [:used-ribbons (conj form-db-path :data :achievement)])
        ribbons-data (map ribbon/fetch-ribbon-data used-ribbons)]
    (when (-> ribbons-data first :id)
      [:<>
       [:h3 [tr :string.menu/ribbon-library]]
       (into [:ul]
             (keep (fn [ribbon]
                     (when (:id ribbon)
                       ^{:key ribbon}
                       [attribution/for-ribbon
                        {:path [:context :ribbon-data]
                         :ribbon-data ribbon}])))
             ribbons-data)])))

(defn- attribution [form-db-path]
  (let [attribution-data (attribution/for-arms {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/title]]
     [:div {:style {:padding-left "1em"}}
      attribution-data]
     [charge-attribution form-db-path]
     [ribbon-attribution form-db-path]]))

(defn- render-achievement []
  [achievement/render (c/++ base-context :data :achievement)])

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
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [render-achievement]
   [:<>
    [ui/selected-component]
    [message/display entity-type]
    [buttons/buttons entity-type]
    [blazonry form-db-path]
    [attribution form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj form-db-path :data :achievement :render-options)
                        :spacer
                        (conj form-db-path :data :achievement :helms)
                        :spacer
                        (conj form-db-path :data :achievement :coat-of-arms)
                        :spacer
                        (conj form-db-path :data :achievement :ornaments)]]]))

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
