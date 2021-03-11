(ns heraldry.frontend.collection-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.config :as config]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.attribution :as attribution]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.form.render-options :as render-options]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.util :refer [id-for-url]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]
            [taoensso.timbre :as log]))

(def form-db-path
  [:collection-form])

(def saved-data-db-path
  [:saved-collection-data])

(def list-db-path
  [:collection-list])

(defn fetch-collection-list-by-user [user-id]
  (go
    (try
      (let [user-data (user/data)]
        (-> (api-request/call
             :fetch-collections-for-use
             {:user-id user-id}
             user-data)
            <?
            :collection))
      (catch :default e
        (log/error "fetch collection list error:" e)))))

(defn fetch-collection [collection-id version]
  (go
    (try
      (let [user-data (user/data)
            full-data (<? (api-request/call :fetch-collection {:id      collection-id
                                                               :version version} user-data))]
        (rf/dispatch [:set saved-data-db-path full-data])
        full-data)
      (catch :default e
        (log/error "fetch collection error:" e)))))

;; views

(defn save-collection-clicked []
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (try
      (let [payload       @(rf/subscribe [:get form-db-path])
            user-data     (user/data)
            response      (<? (api-request/call :save-collection payload user-data))
            collection-id (-> response :collection-id)]
        (rf/dispatch-sync [:set (conj form-db-path :id) collection-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (state/invalidate-cache-without-current form-db-path [collection-id nil])
        (state/invalidate-cache-without-current form-db-path [collection-id 0])
        (rf/dispatch-sync [:set list-db-path nil])
        (state/invalidate-cache list-db-path (:user-id user-data))
        (rf/dispatch-sync [:set-form-message form-db-path (str "Collection saved, new version: " (:version response))])
        (reife/push-state :edit-collection-by-id {:id (id-for-url collection-id)}))
      (catch :default e
        (log/error "save-form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])))))

(defn collection-form []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message  @(rf/subscribe [:get-form-message form-db-path])
        on-submit     (fn [event]
                        (.preventDefault event)
                        (.stopPropagation event)
                        (save-collection-clicked))
        logged-in?    (-> (user/data)
                          :logged-in?)]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2 {:style {:position "fixed"}}]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width       "45%"}}
      [attribution/form (conj form-db-path :attribution)]
      [:form.pure-form.pure-form-aligned
       {:style        {:display "inline-block"}
        :on-key-press (fn [event]
                        (when (-> event .-code (= "Enter"))
                          (on-submit event)))
        :on-submit    on-submit}
       [:fieldset
        [form/field (conj form-db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for   "name"
                     :style {:width "6em"}} "Name"]
            [:input {:id        "name"
                     :value     value
                     :on-change on-change
                     :type      "text"
                     :style     {:margin-right "0.5em"}}]
            [form/checkbox (conj form-db-path :is-public) "Make public"
             :style {:width "7em"}]])]]
       (when form-message
         [:div.form-message form-message])
       (when error-message
         [:div.error-message error-message])
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button.pure-button
         {:type     "button"
          :on-click (let [match  @(rf/subscribe [:get [:route-match]])
                          route  (-> match :data :name)
                          params (-> match :parameters :path)]
                      (cond
                        (= route :edit-collection-by-id) #(reife/push-state :view-collection-by-id params)
                        (= route :create-collection)     #(reife/push-state :collection params)
                        :else                            nil))
          :style    {:margin-right "5px"}}
         "View"]
        (let [disabled? (not logged-in?)]
          [:button.pure-button.pure-button-primary {:type  "submit"
                                                    :class (when disabled? "disabled")}
           "Save"])
        [:div.spacer]]]
      [render-options/form (conj form-db-path :render-options)]]]))

(defn collection-display [collection-id version]
  (let [user-data                (user/data)
        [status collection-data] (state/async-fetch-data
                                  form-db-path
                                  [collection-id version]
                                  #(fetch-collection collection-id version))
        collection-id            (-> collection-data
                                     :id
                                     id-for-url)]
    (when (= status :done)
      [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                   (rf/dispatch [:ui-submenu-close-all])
                                   (.stopPropagation %))}
       [:div.pure-u-1-2 {:style {:margin-left "50%"
                                 :width       "45%"}}
        [:div.credits
         [credits/for-collection collection-data]]
        [:div.pure-control-group {:style {:text-align    "right"
                                          :margin-top    "10px"
                                          :margin-bottom "10px"}}

         (when (or (= (:username collection-data)
                      (:username user-data))
                   ((config/get :admins) (:username user-data)))
           [:button.pure-button.pure-button-primary {:type     "button"
                                                     :on-click #(do
                                                                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                  (rf/dispatch-sync [:clear-form-message form-db-path])
                                                                  (reife/push-state :edit-collection-by-id {:id collection-id}))}
            "Edit"])
         [:div.spacer]]
        [render-options/form (conj form-db-path :render-options)]]])))

(defn list-collection-for-user [user-id]
  (let [[status collection-list] (state/async-fetch-data
                                  list-db-path
                                  user-id
                                  #(fetch-collection-list-by-user user-id))]
    (if (= status :done)
      (if (empty? collection-list)
        [:div "None"]
        [:ul.collection-list
         (doall
          (for [collection collection-list]
            ^{:key (:id collection)}
            [:li.collection
             (let [collection-id (-> collection
                                     :id
                                     id-for-url)]
               [:a {:href     (reife/href :view-collection-by-id {:id collection-id})
                    :on-click #(do
                                 (rf/dispatch-sync [:clear-form-errors form-db-path])
                                 (rf/dispatch-sync [:clear-form-message form-db-path]))}
                (:name collection)])]))])
      [:div "loading..."])))

(defn invalidate-collection-cache [user-id]
  (state/invalidate-cache list-db-path user-id))

(defn list-my-collection []
  (let [user-data (user/data)]
    [:div {:style {:padding "15px"}}
     [:div.pure-u-1-2 {:style {:display    "block"
                               :text-align "justify"
                               :min-width  "30em"}}
      [:p
       "Here you can create collections of coats of arms. Right now you can only browse your own collections. "
       "You explicitly have to save your collection as "
       [:b "public"] ", if you want to share the link and allow others to view it."]]
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:clear-form-errors form-db-path])
                    (rf/dispatch-sync [:clear-form-message form-db-path])
                    (reife/push-state :create-collection))}
      "Create"]
     (when-let [user-id (:user-id user-data)]
       [:<>
        [:h4 "My collections " [:a {:on-click #(do
                                                 (invalidate-collection-cache user-id)
                                                 (.stopPropagation %))} [:i.fas.fa-sync-alt]]]
        [list-collection-for-user user-id]])]))

(defn create-collection [match]
  (rf/dispatch [:set [:route-match] match])
  (let [[status _collection-form-data] (state/async-fetch-data
                                        form-db-path
                                        :new
                                        #(go
                                           {:render-options {:component :render-options
                                                             :mode      :colours
                                                             :outline?  false
                                                             :squiggly? false
                                                             :ui        {:selectable-fields? true}}}))]
    (when (= status :done)
      [collection-form])))

(defn edit-collection [collection-id version]
  (let [[status _collection-form-data] (state/async-fetch-data
                                        form-db-path
                                        [collection-id version]
                                        #(fetch-collection collection-id version))]
    (when (= status :done)
      [collection-form])))

(defn view-list-collection []
  [list-my-collection])

(defn edit-collection-by-id [{:keys [parameters] :as match}]
  (rf/dispatch [:set [:route-match] match])
  (let [id            (-> parameters :path :id)
        version       (-> parameters :path :version)
        collection-id (str "collection:" id)]
    [edit-collection collection-id version]))

(defn view-collection-by-id [{:keys [parameters]}]
  (let [id            (-> parameters :path :id)
        version       (-> parameters :path :version)
        collection-id (str "collection:" id)]
    [collection-display collection-id version]))
