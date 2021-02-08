(ns heraldry.frontend.arms-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.charge :as charge]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.util :refer [full-url-for-arms full-url-for-username id-for-url]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]))

(def form-db-path
  [:arms-form])

(def saved-data-db-path
  [:saved-arms-data])

(def list-db-path
  [:arms-list])

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
        (println "fetch-arms-list-by-user error:" e)))))

(defn fetch-arms [arms-id version]
  (go
    (try
      (let [user-data (user/data)
            response (<? (api-request/call :fetch-arms {:id arms-id
                                                        :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url response)))
            full-data (-> response
                          (merge edn-data))]
        (rf/dispatch [:set saved-data-db-path full-data])
        full-data)
      (catch :default e
        (println "fetch-arms error:" e)))))

;; views

(defn charge-credits []
  (let [coat-of-arms-db-path (conj form-db-path :coat-of-arms)
        coat-of-arms @(rf/subscribe [:get coat-of-arms-db-path])
        used-charges (->> coat-of-arms
                          (tree-seq #(or (map? %)
                                         (vector? %)
                                         (seq? %)) seq)
                          (filter #(and (map? %)
                                        (-> % :component (= :charge))
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
        username (-> (user/data)
                     :username)]
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
          [:g {:filter "url(#shadow)"}
           [:g {:transform "translate(10,10) scale(5,5)"}
            result]]]
         [:div.blazonry
          [:span.disclaimer "Blazon (very rudimentary, very beta)"]
          [:div.blazon
           (blazon/encode-field (:field coat-of-arms) :root? true)]]
         [charge-credits]])
      [:<>])))

(defn generate-svg-clicked [db-path]
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id
                                                                 :version
                                                                 :render-options])
            user-data (user/data)
            response (<? (api-request/call :generate-svg-arms payload user-data))]
        (println "generate-svg-arms response" response)
        (js/window.open (:svg-url response)))
      (catch :default e
        (println "generate-svg-arms error:" e)))))

(defn generate-png-clicked [db-path]
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id
                                                                 :version
                                                                 :render-options])
            user-data (user/data)
            response (<? (api-request/call :generate-png-arms payload user-data))]
        (println "generate-png-arms response" response)
        (js/window.open (:png-url response)))
      (catch :default e
        (println "generate-png-arms error:" e)))))

(defn save-arms-clicked []
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (try
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            response (<? (api-request/call :save-arms payload user-data))
            arms-id (-> response :arms-id)]
        (println "save arms response" response)
        (rf/dispatch-sync [:set (conj form-db-path :id) arms-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (state/invalidate-cache-without-current form-db-path [arms-id nil])
        (state/invalidate-cache-without-current form-db-path [arms-id 0])
        (rf/dispatch-sync [:set list-db-path nil])
        (state/invalidate-cache list-db-path (:user-id user-data))
        (rf/dispatch-sync [:set-form-message form-db-path (str "Arms saved, new version: " (:version response))])
        (reife/push-state :edit-arms-by-id {:id (id-for-url arms-id)}))
      (catch :default e
        (println "save-form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])))))

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
        logged-in? (-> (user/data)
                       :logged-in?)]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2 {:style {:position "fixed"}}
      [render-coat-of-arms]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width "45%"}}
      [component/form-attribution (conj form-db-path :attribution)]
      [:form.pure-form.pure-form-aligned
       {:style {:display "inline-block"}
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
        (let [disabled? (not logged-in?)]
          [:button.pure-button.pure-button-primary {:type "submit"
                                                    :class (when disabled? "disabled")}
           "Save"])
        [:div.spacer]]]
      [component/form-render-options (conj form-db-path :render-options)]
      [component/form-for-coat-of-arms (conj form-db-path :coat-of-arms)]]]))

(defn arms-display [arms-id version]
  (let [user-data (user/data)
        [status arms-data] (state/async-fetch-data
                            form-db-path
                            [arms-id version]
                            #(fetch-arms arms-id version))
        arms-id (-> arms-data
                    :id
                    id-for-url)]
    (when (= status :done)
      [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                   (rf/dispatch [:ui-submenu-close-all])
                                   (.stopPropagation %))}
       [:div.pure-u-1-2 {:style {:position "fixed"}}
        [render-coat-of-arms]]
       [:div.pure-u-1-2 {:style {:margin-left "50%"
                                 :width "45%"}}
        [:div.credits
         [credits/for-arms arms-data]]
        [:div.pure-control-group {:style {:text-align "right"
                                          :margin-top "10px"
                                          :margin-bottom "10px"}}

         [export-buttons :display]
         (when (= (:username arms-data)
                  (:username user-data))
           [:button.pure-button.pure-button-primary {:type "button"
                                                     :on-click #(do
                                                                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                  (rf/dispatch-sync [:clear-form-message form-db-path])
                                                                  (reife/push-state :edit-arms-by-id {:id arms-id}))}
            "Edit"])
         [:div.spacer]]
        [component/form-render-options (conj form-db-path :render-options)]]])))

(defn list-arms-for-user [user-id]
  (let [[status arms-list] (state/async-fetch-data
                            list-db-path
                            user-id
                            #(fetch-arms-list-by-user user-id))]
    (if (= status :done)
      (if (empty? arms-list)
        [:div "None"]
        [:ul.arms-list
         (doall
          (for [arms arms-list]
            ^{:key (:id arms)}
            [:li.arms
             (let [arms-id (-> arms
                               :id
                               id-for-url)]
               [:a {:href (reife/href :view-arms-by-id {:id arms-id})
                    :on-click #(do
                                 (rf/dispatch-sync [:clear-form-errors form-db-path])
                                 (rf/dispatch-sync [:clear-form-message form-db-path]))}
                (:name arms)])]))])
      [:div "loading..."])))

(defn invalidate-arms-cache [user-id]
  (state/invalidate-cache list-db-path user-id))

(defn list-my-arms []
  (let [user-data (user/data)]
    [:div {:style {:padding "15px"}}
     [:div.pure-u-1-2 {:style {:display "block"
                               :text-align "justify"
                               :min-width "30em"}}
      [:p
       "Here you can create coats of arms. Right now you can only browse your own coats of arms, "
       "but armories/collections are planned. You explicitly have to save your coat of arms as "
       [:b "public"] ", if you want to share the link and allow others to view it."]
      [:p
       "However, SVG/PNG links can be viewed by anyone."]]
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:clear-form-errors form-db-path])
                    (rf/dispatch-sync [:clear-form-message form-db-path])
                    (reife/push-state :create-arms))}
      "Create"]
     (when-let [user-id (:user-id user-data)]
       [:<>
        [:h4 "My arms " [:a {:on-click #(do
                                          (invalidate-arms-cache user-id)
                                          (.stopPropagation %))} [:i.fas.fa-sync-alt]]]
        [list-arms-for-user user-id]])]))

(defn create-arms [match]
  (rf/dispatch [:set [:route-match] match])
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go
                                     {:coat-of-arms default/coat-of-arms
                                      :render-options {:component :render-options
                                                       :mode :colours
                                                       :outline? false
                                                       :squiggly? false
                                                       :ui {:selectable-fields? true}}}))]
    (when (= status :done)
      [arms-form])))

(defn edit-arms [arms-id version]
  (let [[status _arms-form-data] (state/async-fetch-data
                                  form-db-path
                                  [arms-id version]
                                  #(fetch-arms arms-id version))]
    (when (= status :done)
      [arms-form])))

(defn view-list-arms []
  [list-my-arms])

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
