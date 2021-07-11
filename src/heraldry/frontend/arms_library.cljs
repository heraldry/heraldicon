(ns heraldry.frontend.arms-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.config :as config]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.arms-select :as arms-select]
            [heraldry.frontend.form.attribution :as attribution]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.form.render-options :as render-options]
            [heraldry.frontend.form.tag :as tag]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            [heraldry.frontend.user :as user]
            [heraldry.util
             :refer
             [full-url-for-arms full-url-for-username id-for-url]]
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
        [:div {:style {:margin-left "10px"
                       :margin-right "10px"}}
         [:svg {:id "svg"
                :style {:width "25em"}
                :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
                :preserveAspectRatio "xMidYMin slice"}
          [:g {:filter (when (:escutcheon-shadow? render-options)
                         "url(#shadow)")}
           [:g {:transform "translate(10,10) scale(5,5)"}
            result]]]
         [:div.blazonry
          [:span.disclaimer "Blazon (very rudimentary, very beta)"]
          [:div.blazon
           (blazon/encode-field (:field coat-of-arms) :root? true)]]
         [charge-credits]])
      [:<>])))

(defn generate-svg-clicked [db-path]
  (modal/start-loading)
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id
                                                                 :version
                                                                 :render-options])
            user-data (user/data)
            response (<? (api-request/call :generate-svg-arms payload user-data))]
        (js/window.open (:svg-url response))
        (modal/stop-loading))
      (catch :default e
        (log/error "generate svg arms error:" e)
        (modal/stop-loading)))))

(defn generate-png-clicked [db-path]
  (modal/start-loading)
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id
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

(defn save-arms-clicked []
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

(defn export-buttons [mode]
  (let [logged-in? (-> (user/data)
                       :logged-in?)
        unsaved-changes? (not= (-> @(rf/subscribe [:get form-db-path])
                                   (dissoc :render-options))
                               (-> @(rf/subscribe [:get saved-data-db-path])
                                   (dissoc :render-options)))
        disabled? (or (not logged-in?)
                      (and (= mode :form)
                           unsaved-changes?))]
    [:<>
     [:button.pure-button {:type "button"
                           :class (when disabled? "disabled")
                           :on-click (if disabled?
                                       (if (not logged-in?)
                                         #(js/alert "Need to be logged in")
                                         #(js/alert "Save your changes first"))
                                       #(generate-svg-clicked form-db-path))
                           :style {:float "left"}}
      "SVG Link"]
     [:button.pure-button {:type "button"
                           :class (when disabled? "disabled")
                           :on-click (if disabled?
                                       (if (not logged-in?)
                                         #(js/alert "Need to be logged in")
                                         #(js/alert "Save your changes first"))
                                       #(generate-png-clicked form-db-path))
                           :style {:float "left"
                                   :margin-left "5px"}}
      "PNG Link"]]))

(defn arms-form []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (save-arms-clicked))
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        arms-data @(rf/subscribe [:get form-db-path])
        saved-and-owned-by-me? (and (:id arms-data)
                                    (= (:username user-data) (:username arms-data)))]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2.no-scrollbar {:style {:position "fixed"
                                            :height "100vh"
                                            :overflow-y "scroll"}}
      [render-coat-of-arms]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width "45%"}}
      #_[attribution/form (conj form-db-path :attribution)]
      #_[:form.pure-form.pure-form-aligned
         {:style {:display "inline-block"
                  :width "100%"}
          :on-key-press (fn [event]
                          (when (-> event .-code (= "Enter"))
                            (on-submit event)))
          :on-submit on-submit}
         [:fieldset
          [form/field (conj form-db-path :name)
           (fn [& {:keys [value on-change]}]
             [:div.pure-control-group
              [:label {:for "name"
                       :style {:width "6em"}} "Name"]
              [:input {:id "name"
                       :value value
                       :on-change on-change
                       :type "text"
                       :style {:margin-right "0.5em"}}]
              [form/checkbox (conj form-db-path :is-public) "Make public"
               :style {:width "7em"}]])]]
         [:fieldset
          [tag/form (conj form-db-path :tags)]]
         (when form-message
           [:div.form-message form-message])
         (when error-message
           [:div.error-message error-message])
         [:div.pure-control-group {:style {:text-align "right"
                                           :margin-top "10px"}}
          [export-buttons :form]
          [:button.pure-button.pure-button
           {:type "button"
            :on-click (let [match @(rf/subscribe [:get [:route-match]])
                            route (-> match :data :name)
                            params (-> match :parameters :path)]
                        (cond
                          (= route :edit-arms-by-id) #(reife/push-state :view-arms-by-id params)
                          (= route :create-arms) #(reife/push-state :arms params)
                          :else nil))
            :style {:margin-right "5px"}}
           "View"]
          (when saved-and-owned-by-me?
            [:button.pure-button.pure-button
             {:type "button"
              :style {:margin-right "5px"}
              :on-click #(do
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
                           (reife/push-state :create-arms))}
             "Copy to new"])
          (let [disabled? (not logged-in?)]
            [:button.pure-button.pure-button-primary {:type "submit"
                                                      :class (when disabled? "disabled")}
             "Save"])
          [:div.spacer]]]
      #_[render-options/form (conj form-db-path :render-options)]
      #_[coat-of-arms-component/form (conj form-db-path :coat-of-arms)]
      [ui/selected-component]
      [ui/component-tree [form-db-path
                          (conj form-db-path :render-options)
                          (conj form-db-path :coat-of-arms)]]]]))

(defn arms-display [arms-id version]
  (let [user-data (user/data)
        [status arms-data] (state/async-fetch-data
                            form-db-path
                            [arms-id version]
                            #(arms-select/fetch-arms arms-id version saved-data-db-path))
        arms-id (-> arms-data
                    :id
                    id-for-url)]
    (when (= status :done)
      [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                   (rf/dispatch [:ui-submenu-close-all])
                                   (.stopPropagation %))}
       [:div.pure-u-1-2.no-scrollbar {:style {:position "fixed"
                                              :height "100vh"
                                              :overflow-y "scroll"}}
        [render-coat-of-arms]]
       [:div.pure-u-1-2 {:style {:margin-left "50%"
                                 :width "45%"}}
        [:div.credits
         [credits/for-arms arms-data]]
        [:div.pure-control-group {:style {:text-align "right"
                                          :margin-top "10px"
                                          :margin-bottom "10px"}}

         [export-buttons :display]
         (when (or (= (:username arms-data)
                      (:username user-data))
                   ((config/get :admins) (:username user-data)))
           [:button.pure-button.pure-button-primary {:type "button"
                                                     :on-click #(do
                                                                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                  (rf/dispatch-sync [:clear-form-message form-db-path])
                                                                  (reife/push-state :edit-arms-by-id {:id arms-id}))}
            "Edit"])
         [:div.spacer]]
        [render-options/form (conj form-db-path :render-options)]]])))

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

(defn edit-arms [arms-id version]
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  [arms-id version]
                                  #(arms-select/fetch-arms arms-id version saved-data-db-path))
        selected-path @(rf/subscribe [:ui-component-node-selected-path])
        selected-data (when selected-path
                        @(rf/subscribe [:get-value selected-path]))]
    (when (= status :done)
      (when-not selected-data
        (rf/dispatch [:ui-component-node-select form-db-path]))
      [arms-form])))

(defn edit-arms-by-id [{:keys [parameters] :as match}]
  (rf/dispatch [:set [:route-match] match])
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    [edit-arms arms-id version]))

(defn view-arms-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    [arms-display arms-id version]))
