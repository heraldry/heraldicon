(ns heraldicon.frontend.library.arms
  (:require
   ["copy-to-clipboard" :as copy-to-clipboard]
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.entity.arms :as entity.arms]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.charge :as charge]
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.http :as http]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.ribbon :as ribbon]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.element.arms-select :as arms-select]
   [heraldicon.frontend.ui.element.blazonry-editor :as blazonry-editor]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]
   [heraldicon.frontend.user :as user]
   [heraldicon.heraldry.default :as default]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.render.core :as render]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def form-db-path
  [:arms-form])

(def ^:private saved-data-db-path
  [:saved-arms-data])

(defn- charge-attribution []
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

(defn- ribbon-attribution []
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

(defn- attribution []
  (let [attribution-data (attribution/for-arms {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/title]]
     [:div {:style {:padding-left "1em"}}
      attribution-data]
     [charge-attribution]
     [ribbon-attribution]]))

(defn- base-context []
  (assoc
   context/default
   :path form-db-path
   :render-options-path (conj form-db-path :data :achievement :render-options)
   :select-component-fn (fn [event context]
                          (state/dispatch-on-event event [:ui-component-node-select (:path context)]))))

(defn- render-achievement []
  [render/achievement (c/++ (base-context) :data :achievement)])

(defn- blazonry []
  [:div.blazonry
   [:h3
    [tr :string.entity/blazon]
    [:span {:style {:font-size "0.75em"}}
     " " [tr :string.miscellaneous/beta-blazon]]]
   [:div.blazon
    (string/tr-raw (interface/blazon (assoc context/default
                                            :path (conj form-db-path :data :achievement :coat-of-arms))) :en)]])

(defn- generate-svg-clicked []
  (modal/start-loading)
  (go
    (try
      (let [full-data @(rf/subscribe [:get form-db-path])
            payload (-> full-data
                        (select-keys [:id :version])
                        (assoc :render-options (get-in full-data [:data :achievement :render-options])))
            user-data (user/data)
            response (<? (api.request/call :generate-svg-arms payload user-data))]
        (js/window.open (:svg-url response))
        (modal/stop-loading))
      (catch :default e
        (log/error "generate svg arms error:" e)
        (modal/stop-loading)))))

(defn- generate-png-clicked []
  (modal/start-loading)
  (go
    (try
      (let [full-data @(rf/subscribe [:get form-db-path])
            payload (-> full-data
                        (select-keys [:id :version])
                        (assoc :render-options (get-in full-data [:data :achievement :render-options])))
            user-data (user/data)
            response (<? (api.request/call :generate-png-arms payload user-data))]
        (js/window.open (:png-url response))
        (modal/stop-loading))
      (catch :default e
        (log/error "generate png arms error:" e)
        (modal/stop-loading)))))

(defn- invalidate-arms-cache [user-id]
  (state/invalidate-cache arms-select/list-db-path user-id))

(defn- save-arms-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (try
      (modal/start-loading)
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            user-id (:user-id user-data)
            response (<? (api.request/call :save-arms payload user-data))
            arms-id (:id response)]
        (rf/dispatch-sync [:set (conj form-db-path :id) arms-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (state/invalidate-cache-without-current form-db-path [arms-id nil])
        (state/invalidate-cache-without-current form-db-path [arms-id 0])
        (rf/dispatch-sync [:set arms-select/list-db-path nil])
        (invalidate-arms-cache user-id)
        (invalidate-arms-cache :all)
        (rf/dispatch-sync [:set-form-message form-db-path
                           (string/str-tr :string.user.message/arms-saved " " (:version response))])
        (reife/push-state :route.arms/details-by-id {:id (id/for-url arms-id)}))
      (modal/stop-loading)
      (catch :default e
        (log/error "save form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
        (modal/stop-loading)))))

(defn- copy-to-new-clicked []
  (let [arms-data @(rf/subscribe [:get form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:set saved-data-db-path nil])
    (state/set-async-fetch-data
     form-db-path
     :new
     (dissoc arms-data
             :id
             :version
             :latest-version
             :username
             :user-id
             :created-at
             :first-version-created-at
             :name))
    (rf/dispatch-sync [:set-form-message form-db-path :string.user.message/created-unsaved-copy])
    (reife/push-state :route.arms/create)))

(defn- share-button-clicked []
  (let [short-url (entity.arms/short-url @(rf/subscribe [:get form-db-path]))]
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (if (copy-to-clipboard short-url)
      (rf/dispatch [:set-form-message form-db-path :string.user.message/copied-url-for-sharing])
      (rf/dispatch [:set-form-error form-db-path :string.user.message/copy-to-clipboard-failed]))))

(defn- button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        arms-id @(rf/subscribe [:get (conj form-db-path :id)])
        arms-username @(rf/subscribe [:get (conj form-db-path :username)])
        public? (= @(rf/subscribe [:get (conj form-db-path :access)])
                   :public)
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= (update-in @(rf/subscribe [:get form-db-path]) [:data :achievement]
                                          dissoc :render-options)
                               (update-in @(rf/subscribe [:get saved-data-db-path]) [:data :achievement]
                                          dissoc :render-options))
        can-export? (and logged-in?
                         (not unsaved-changes?))
        saved? arms-id
        owned-by-me? (= (:username user-data) arms-username)
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))
        can-share? (and public?
                        saved?
                        (not unsaved-changes?))]
    [:<>
     (when form-message
       [:div.success-message [tr form-message]])
     (when error-message
       [:div.error-message [tr error-message]])

     [:div.buttons {:style {:display "flex"}}
      [:div {:style {:flex "auto"}}]
      [:button.button {:style {:flex "initial"
                               :color "#777"}
                       :class (when-not can-share? "disabled")
                       :title (when-not can-share? (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))
                       :on-click (when can-share?
                                   share-button-clicked)}
       [:i.fas.fa-share-alt]]
      [hover-menu/hover-menu
       {:path [:arms-form-action-menu]}
       :string.button/actions
       [{:title (string/str-tr :string.button/export " (SVG)")
         :icon "fas fa-file-export"
         :handler generate-svg-clicked
         :disabled? (not can-export?)
         :tooltip (when-not can-export?
                    (if (not logged-in?)
                      (tr :string.user.message/need-to-be-logged-in)
                      (tr :string.user.message/save-changes-first)))}
        {:title (string/str-tr :string.button/export " (PNG)")
         :icon "fas fa-file-export"
         :handler generate-png-clicked
         :disabled? (not can-export?)
         :tooltip (when-not can-export?
                    (if (not logged-in?)
                      (tr :string.user.message/need-to-be-logged-in)
                      (tr :string.user.message/save-changes-first)))}
        {:title :string.button/share
         :icon "fas fa-share-alt"
         :handler share-button-clicked
         :disabled? (not can-share?)
         :tooltip (when-not can-share?
                    (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))}
        {:title :string.button/copy-to-new
         :icon "fas fa-clone"
         :handler copy-to-new-clicked
         :disabled? (not can-copy?)
         :tooltip (when-not can-copy?
                    (tr :string.user.message/need-to-be-logged-in-and-arms-must-be-saved))}]
       [:button.button {:style {:flex "initial"
                                :color "#777"
                                :margin-left "10px"}}
        [:i.fas.fa-ellipsis-h]]
       :require-click? true]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-arms-clicked
                                           #(js/alert (tr :string.user.message/need-to-be-logged-in-and-own-the-arms)))
                               :style {:flex "initial"
                                       :margin-left "10px"}}
       [tr :string.button/save]]]]))

(defn- arms-form []
  (rf/dispatch [:set-title-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-arms])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [render-achievement]
   [:<>
    [ui/selected-component]
    [button-row]
    [blazonry]
    [attribution]]
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

(defn- load-arms [arms-id version]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path arms-id])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path arms-id]))
  (let [[status arms-data] (state/async-fetch-data
                            form-db-path
                            [arms-id version]
                            #(api/fetch-arms arms-id version saved-data-db-path))]
    (when (= status :done)
      (if arms-data
        [arms-form]
        [not-found/not-found]))))

(defn on-select [{:keys [id]}]
  {:href (reife/href :route.arms/details-by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [:clear-form-errors form-db-path])
               (rf/dispatch-sync [:clear-form-message form-db-path]))})

(defn list-view []
  (rf/dispatch [:set-title :string.entity/arms])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p [tr :string.text.arms-library/create-and-view-arms]]
    [:p [tr :string.text.arms-library/svg-png-access-info]]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :route.arms/create))}
    [tr :string.button/create]]
   " "
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :route.arms/create)
                  (blazonry-editor/open (c/++ (base-context) :data :achievement :coat-of-arms :field)))}
    [tr :string.button/create-from-blazon]]
   [:div {:style {:padding-top "0.5em"}}
    [arms-select/list-arms on-select]]])

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
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path nil]))
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go (<? (load-hdn (:base query-params)))))]
    (when (= status :done)
      [arms-form])))

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [load-arms (str "arms:" id) version])
