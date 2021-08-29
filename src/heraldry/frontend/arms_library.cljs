(ns heraldry.frontend.arms-library
  (:require ["copy-to-clipboard" :as copy-to-clipboard]
            [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.attribution :as attribution]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.ribbon :as ribbon]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            [heraldry.frontend.ui.element.arms-select :as arms-select]
            [heraldry.frontend.undo :as undo]
            [heraldry.frontend.user :as user]
            [heraldry.interface :as interface]
            [heraldry.render :as render]
            [heraldry.util :as util]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]
            [taoensso.timbre :as log]))

(def form-db-path
  [:arms-form])

(def saved-data-db-path
  [:saved-arms-data])

;; views

(defn charge-attribution []
  (let [used-charges @(rf/subscribe [:used-charge-variants form-db-path])
        charges-data (->> used-charges
                          (map charge/fetch-charge-data))]
    (when (-> charges-data first :id)
      [:<>
       [:h3 "Charges"]
       [:ul
        (doall
         (for [charge charges-data]
           (when-let [charge-id (:id charge)]
             ^{:key charge-id}
             [attribution/for-charge
              [:context :charge-data]
              {:charge-data charge}])))]])))

(defn ribbon-attribution []
  (let [used-ribbons @(rf/subscribe [:used-ribbons form-db-path])
        ribbons-data (->> used-ribbons
                          (map ribbon/fetch-ribbon-data))]
    (when (-> ribbons-data first :id)
      [:<>
       [:h3 "Ribbons"]
       [:ul
        (doall
         (for [ribbon ribbons-data]
           (when-let [ribbon-id (:id ribbon)]
             ^{:key ribbon-id}
             [attribution/for-ribbon
              [:context :ribbon-data]
              {:ribbon-data ribbon}])))]])))

(defn attribution []
  (let [attribution-data (attribution/for-arms form-db-path {})]
    [:div.attribution
     [:h3 "Attribution"]
     [:div {:style {:padding-left "1em"}}
      attribution-data]
     [charge-attribution]
     [ribbon-attribution]]))

(defn render-coat-of-arms []
  [render/achievement
   form-db-path
   (assoc
    context/default
    :render-options-path (conj form-db-path :render-options))])

(defn blazonry []
  [:div.blazonry
   [:h3 "Blazon" [:span {:style {:font-size "0.75em"}} " (beta, not complete)"]]
   [:div.blazon
    (interface/blazon (conj form-db-path :coat-of-arms) {})]])

(defn generate-svg-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (modal/start-loading)
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get-value form-db-path]) [:id
                                                                            :version
                                                                            :render-options])
            user-data (user/data)
            response (<? (api-request/call :generate-svg-arms payload user-data))]
        (js/window.open (:svg-url response))
        (modal/stop-loading))
      (catch :default e
        (log/error "generate svg arms error:" e)
        (modal/stop-loading)))))

(defn generate-png-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (modal/start-loading)
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get-value form-db-path]) [:id
                                                                            :version
                                                                            :render-options])
            user-data (user/data)
            response (<? (api-request/call :generate-png-arms payload user-data))]
        (js/window.open (:png-url response))
        (modal/stop-loading))
      (catch :default e
        (log/error "generate png arms error:" e)
        (modal/stop-loading)))))

(defn invalidate-arms-cache [user-id]
  (state/invalidate-cache arms-select/list-db-path user-id))

(defn save-arms-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (try
      (modal/start-loading)
      (let [payload @(rf/subscribe [:get-value form-db-path])
            user-data (user/data)
            user-id (:user-id user-data)
            response (<? (api-request/call :save-arms payload user-data))
            arms-id (-> response :arms-id)]
        (rf/dispatch-sync [:set (conj form-db-path :id) arms-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get-value form-db-path])])
        (state/invalidate-cache-without-current form-db-path [arms-id nil])
        (state/invalidate-cache-without-current form-db-path [arms-id 0])
        (rf/dispatch-sync [:set arms-select/list-db-path nil])
        (invalidate-arms-cache user-id)
        (invalidate-arms-cache :all)
        (rf/dispatch-sync [:set-form-message form-db-path (str "Arms saved, new version: " (:version response))])
        (reife/push-state :view-arms-by-id {:id (util/id-for-url arms-id)}))
      (modal/stop-loading)
      (catch :default e
        (log/error "save form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
        (modal/stop-loading)))))

(defn copy-to-new-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [arms-data @(rf/subscribe [:get-value form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:set saved-data-db-path nil])
    (state/set-async-fetch-data
     form-db-path
     :new
     (-> arms-data
         (dissoc :id)
         (dissoc :version)
         (dissoc :latest-version)
         (dissoc :username)
         (dissoc :user-id)
         (dissoc :created-at)
         (dissoc :first-version-created-at)
         (dissoc :is-current-version)
         (dissoc :name)))
    (rf/dispatch-sync [:set-form-message form-db-path "Created an unsaved copy."])
    (reife/push-state :create-arms)))

(defn share-button-clicked [_event]
  (let [short-url (util/short-url @(rf/subscribe [:get-value form-db-path]))]
    (copy-to-clipboard short-url)
    (rf/dispatch [:set-form-message form-db-path "Copied URL for sharing."])))

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        arms-id @(rf/subscribe [:get-value (conj form-db-path :id)])
        arms-username @(rf/subscribe [:get-value (conj form-db-path :username)])
        is-public @(rf/subscribe [:get-value (conj form-db-path :is-public)])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= (-> @(rf/subscribe [:get-value form-db-path])
                                   (dissoc :render-options))
                               (-> @(rf/subscribe [:get-value saved-data-db-path])
                                   (dissoc :render-options)))
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
        can-share? (and is-public
                        saved?
                        (not unsaved-changes?))]
    [:<>
     (when form-message
       [:div.success-message form-message])
     (when error-message
       [:div.error-message error-message])

     [:div.buttons {:style {:display "flex"}}
      [:button.button {:type "button"
                       :class (when-not can-export? "disabled")
                       :title (when-not can-export? "Arms need to be public and saved for exporting.")
                       :on-click (if can-export?
                                   generate-svg-clicked
                                   (if (not logged-in?)
                                     #(js/alert "Need to be logged in")
                                     #(js/alert "Save your changes first")))
                       :style {:flex "initial"
                               :margin-right "10px"}}
       "SVG"]
      [:button.button {:type "button"
                       :class (when-not can-export? "disabled")
                       :title (when-not can-export? "Arms need to be public and saved for exporting.")
                       :on-click (if can-export?
                                   generate-png-clicked
                                   (if (not logged-in?)
                                     #(js/alert "Need to be logged in")
                                     #(js/alert "Save your changes first")))
                       :style {:flex "initial"
                               :margin-right "10px"}}
       "PNG"]
      (when arms-id
        [:button.button {:style {:flex "initial"
                                 :color "#777"}
                         :class (when-not can-share? "disabled")
                         :title (when-not can-share? "Arms need to be public and saved for sharing.")
                         :on-click share-button-clicked}
         [:i.fas.fa-share-alt]])
      [:div {:style {:flex "auto"}}]
      [:button.button
       {:type "button"
        :class (when-not can-copy? "disabled")
        :style {:flex "initial"
                :margin-left "10px"}
        :on-click (if can-copy?
                    copy-to-new-clicked
                    #(js/alert "Need to be logged in and arms must be saved."))}
       "Copy to new"]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-arms-clicked
                                           #(js/alert "Need to be logged in and own the arms."))
                               :style {:flex "initial"
                                       :margin-left "10px"}}
       "Save"]]]))

(defn arms-form []
  (if @(rf/subscribe [:get-value (conj form-db-path :id)])
    (rf/dispatch [:set-title @(rf/subscribe [:get-value (conj form-db-path :name)])])
    (rf/dispatch [:set-title "Create Arms"]))
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(27em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"}}
    [render-coat-of-arms]]
   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"
                               :position "relative"}}
    [ui/selected-component]
    [button-row]
    [blazonry]
    [attribution]]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"
                               :position :relative}}
    [undo/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj form-db-path :render-options)
                        :spacer
                        (conj form-db-path :helms)
                        :spacer
                        (conj form-db-path :mottos)
                        :spacer
                        (conj form-db-path :coat-of-arms)]]]])

(defn arms-display [arms-id version]
  (let [[status arms-data] (state/async-fetch-data
                            form-db-path
                            [arms-id version]
                            #(arms-select/fetch-arms arms-id version saved-data-db-path))]
    (when (= status :done)
      (if arms-data
        [arms-form]
        [:div "Not found"]))))

(defn link-to-arms [arms]
  (let [arms-id (-> arms
                    :id
                    util/id-for-url)]
    [:a {:href (reife/href :view-arms-by-id {:id arms-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (:name arms)]))

(defn list-all-arms []
  [arms-select/list-arms link-to-arms])

(defn view-list-arms []
  (rf/dispatch [:set-title "Arms"])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [:p
     "Here you can create and view coats of arms. You explicitly have to save your coat of arms as "
     [:b "public"] " and add a license, if you want to share the link and allow others to view it."]
    [:p
     "However, SVG/PNG links can be viewed by anyone."]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-arms))}
    "Create"]
   [:div {:style {:padding-top "0.5em"}}
    [list-all-arms]]])

(defn create-arms [_match]
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go
                                     {:coat-of-arms default/coat-of-arms
                                      :render-options default/render-options}))]
    (when (= status :done)
      [arms-form])))

(defn view-arms-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    [arms-display arms-id version]))
