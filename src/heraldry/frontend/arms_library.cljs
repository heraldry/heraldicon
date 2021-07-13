(ns heraldry.frontend.arms-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.arms-select :as arms-select]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            [heraldry.frontend.user :as user]
            [heraldry.util :refer [full-url-for-arms full-url-for-username id-for-url]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]
            [taoensso.timbre :as log]))

(def form-db-path
  [:arms-form])

(def saved-data-db-path
  [:saved-arms-data])

;; views

(defn charge-credits []
  (let [coat-of-arms-db-path (conj form-db-path :coat-of-arms)
        coat-of-arms @(rf/subscribe [:get coat-of-arms-db-path])
        used-charges (->> coat-of-arms
                          (tree-seq #(or (map? %)
                                         (vector? %)
                                         (seq? %)) seq)
                          (filter #(and (map? %)
                                        (some-> % :type namespace (= "heraldry.charge.type"))
                                        (-> % :variant :version)
                                        (-> % :variant :id)))
                          (map :variant)
                          set)
        charges-data (->> used-charges
                          (map charge/fetch-charge-data))]
    (when (-> charges-data first :id)
      [:div.credits
       [:span.credits-heading "Charge attribution"]
       [:ul
        (for [charge charges-data]
          (when-let [charge-id (:id charge)]
            ^{:key charge-id}
            [:li [credits/for-charge charge]]))]])))

(defn render-coat-of-arms []
  (let [coat-of-arms-db-path (conj form-db-path :coat-of-arms)
        coat-of-arms @(rf/subscribe [:get coat-of-arms-db-path])
        render-options @(rf/subscribe [:get (conj form-db-path :render-options)])
        arms-data @(rf/subscribe [:get form-db-path])
        attribution (:attribution arms-data)
        name (:name arms-data)
        arms-url (full-url-for-arms arms-data)
        username (:username arms-data)]
    (if coat-of-arms
      (let [{:keys [result
                    environment]} (render/coat-of-arms
                                   coat-of-arms
                                   100
                                   (merge
                                    context/default
                                    {:render-options render-options
                                     :db-path coat-of-arms-db-path
                                     :metadata [metadata/attribution name username (full-url-for-username username) arms-url attribution]}))
            {:keys [width height]} environment]
        [:svg {:id "svg"
               :style {:width "100%"
                       :height "100%"}
               :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
               :preserveAspectRatio "xMidYMin meet"}
         [:g {:filter (when (:escutcheon-shadow? render-options)
                        "url(#shadow)")}
          [:g {:transform "translate(10,10) scale(5,5)"}
           result]]])
      [:<>])))

(defn blazonry []
  (let [coat-of-arms-db-path (conj form-db-path :coat-of-arms)
        coat-of-arms @(rf/subscribe [:get coat-of-arms-db-path])]
    [:div.blazonry
     [:span.disclaimer "Blazon (very rudimentary, very beta)"]
     [:div.blazon
      (blazon/encode-field (:field coat-of-arms) :root? true)]]))

(defn attribution []
  (let [arms-data @(rf/subscribe [:get form-db-path])]
    [:<>
     [:div.credits
      [credits/for-arms arms-data]]
     [charge-credits]]))

(defn generate-svg-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (modal/start-loading)
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get form-db-path]) [:id
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
      (let [payload (select-keys @(rf/subscribe [:get form-db-path]) [:id
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
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            user-id (:user-id user-data)
            response (<? (api-request/call :save-arms payload user-data))
            arms-id (-> response :arms-id)]
        (rf/dispatch-sync [:set (conj form-db-path :id) arms-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (state/invalidate-cache-without-current form-db-path [arms-id nil])
        (state/invalidate-cache-without-current form-db-path [arms-id 0])
        (rf/dispatch-sync [:set arms-select/list-db-path nil])
        (invalidate-arms-cache user-id)
        (invalidate-arms-cache :all)
        (rf/dispatch-sync [:set-form-message form-db-path (str "Arms saved, new version: " (:version response))])
        (reife/push-state :edit-arms-by-id {:id (id-for-url arms-id)}))
      (modal/stop-loading)
      (catch :default e
        (log/error "save form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
        (modal/stop-loading)))))

(defn copy-to-new-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [arms-data @(rf/subscribe [:get form-db-path])]
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

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        arms-data @(rf/subscribe [:get form-db-path])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= (-> @(rf/subscribe [:get form-db-path])
                                   (dissoc :render-options))
                               (-> @(rf/subscribe [:get saved-data-db-path])
                                   (dissoc :render-options)))
        can-export? (and logged-in?
                         (not unsaved-changes?))
        saved? (:id arms-data)
        owned-by-me? (= (:username user-data) (:username arms-data))
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))]
    [:<>
     (when form-message
       [:div.success-message form-message])
     (when error-message
       [:div.error-message error-message])

     [:div.buttons {:style {:display "flex"
                            :gap "10px"}}
      [:button.button {:type "button"
                       :class (when-not can-export? "disabled")
                       :on-click (if can-export?
                                   generate-svg-clicked
                                   (if (not logged-in?)
                                     #(js/alert "Need to be logged in")
                                     #(js/alert "Save your changes first")))
                       :style {:flex 1}}
       "SVG Link"]
      [:button.button {:type "button"
                       :class (when-not can-export? "disabled")
                       :on-click (if can-export?
                                   generate-png-clicked
                                   (if (not logged-in?)
                                     #(js/alert "Need to be logged in")
                                     #(js/alert "Save your changes first")))
                       :style {:flex 1}}
       "PNG Link"]
      [:div.spacer {:style {:flex 10}}]
      [:button.button
       {:type "button"
        :class (when-not can-copy? "disabled")
        :style {:flex 1}
        :on-click (if can-copy?
                    copy-to-new-clicked
                    #(js/alert "Need to be logged in and arms must be saved."))}
       "Copy to new"]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-arms-clicked
                                           #(js/alert "Need to be logged in and own the arms."))}
       "Save"]]]))

(defn arms-form []
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] 33% [second] 25% [end]"
                 :grid-template-rows "[top] 50% [middle] 25% [bottom-half] 25% [bottom]"
                 :grid-template-areas "'arms selected-component component-tree'
                                       'arms attribution component-tree'
                                       'blazonry attribution component-tree'"
                 :padding-left "10px"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "arms"
                               :overflow-y "scroll"}}
    [render-coat-of-arms]]
   [:div {:style {:grid-area "blazonry"}}
    [blazonry]]
   [:div {:style {:grid-area "selected-component"
                  :padding-top "10px"}}
    [ui/selected-component]
    [button-row]]
   [:div {:style {:grid-area "attribution"}}
    [attribution]]
   [:div {:style {:grid-area "component-tree"
                  :padding-top "5px"}}
    [ui/component-tree [form-db-path
                        (conj form-db-path :render-options)
                        (conj form-db-path :coat-of-arms)]]]])

(defn arms-display [arms-id version]
  (let [[status _] (state/async-fetch-data
                    form-db-path
                    [arms-id version]
                    #(arms-select/fetch-arms arms-id version saved-data-db-path))]
    (when (= status :done)
      [arms-form])))

(defn link-to-arms [arms]
  (let [arms-id (-> arms
                    :id
                    id-for-url)]
    [:a {:href (reife/href :view-arms-by-id {:id arms-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (:name arms)]))

(defn list-all-arms []
  [arms-select/list-arms link-to-arms])

(defn view-list-arms []
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :min-width "30em"}}
    [:p
     "Here you can create and view coats of arms. You explicitly have to save your coat of arms as "
     [:b "public"] " and add a license, if you want to share the link and allow others to view it."]
    [:p
     "However, SVG/PNG links can be viewed by anyone."]]
   [:button.pure-button.pure-button-primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-arms))}
    "Create"]
   [:div {:style {:padding-top "0.5em"}}
    [list-all-arms]]])

(defn create-arms [match]
  (rf/dispatch [:set [:route-match] match])
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go
                                     {:coat-of-arms default/coat-of-arms
                                      :render-options default/render-options}))]
    (when (= status :done)
      [arms-form])))

(defn edit-arms-by-id [{:keys [parameters] :as match}]
  (rf/dispatch [:set [:route-match] match])
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    [arms-display arms-id version :edit? true]))

(defn view-arms-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    [arms-display arms-id version]))
