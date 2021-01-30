(ns heraldry.frontend.arms-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.charge-map :as charge-map]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.http :as http]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [heraldry.util :refer [full-url-for-arms id-for-url]]
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
        (rf/dispatch-sync [:set list-db-path :loading])
        (rf/dispatch-sync [:set list-db-path (-> (api-request/call
                                                  :fetch-arms-for-user
                                                  {:user-id user-id}
                                                  user-data)
                                                 <?
                                                 :arms)]))
      (catch :default e
        (println "fetch-arms-list-by-user error:" e)))))

(defn fetch-arms-and-fill-form [arms-id version]
  (go
    (try
      (rf/dispatch-sync [:set form-db-path :loading])
      (let [user-data (user/data)
            response (<? (api-request/call :fetch-arms {:id arms-id
                                                        :version version} user-data))
            edn-data (<? (http/fetch (:edn-data-url response)))]
        (rf/dispatch [:set saved-data-db-path (-> response
                                                  (merge edn-data))])
        (rf/dispatch [:set form-db-path (-> response
                                            (merge edn-data))]))
      (catch :default e
        (println ":fetch-arms-by-id error:" e)))))

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
                          (map charge-map/fetch-charge-data))]
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
                                     :metadata [metadata/attribution name username arms-url attribution]}))
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
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id :version])
            user-data (user/data)
            response (<? (api-request/call :generate-svg-arms payload user-data))]
        (println "generate-svg-arms response" response)
        (js/window.open (:svg-url response)))
      (catch :default e
        (println "generate-svg-arms error:" e)))))

(defn generate-png-clicked [db-path]
  (go
    (try
      (let [payload (select-keys @(rf/subscribe [:get db-path]) [:id :version])
            user-data (user/data)
            response (<? (api-request/call :generate-png-arms payload user-data))]
        (println "generate-png-arms response" response)
        (js/window.open (:png-url response)))
      (catch :default e
        (println "generate-png-arms error:" e)))))

(defn save-arms-clicked []
  (go
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (try
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            response (<? (api-request/call :save-arms payload user-data))
            arms-id (-> response :arms-id)]
        (println "save arms response" response)
        (rf/dispatch-sync [:set (conj form-db-path :id) arms-id])
        (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
        (reife/push-state :edit-arms-by-id {:id (id-for-url arms-id)}))
      (catch :default e
        (println "save-form error:" e)
        (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])))))

(defn arms-form []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (save-arms-clicked))]
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
       (when error-message
         [:div.error-message error-message])
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
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        (let [disabled? (not= @(rf/subscribe [:get form-db-path])
                              @(rf/subscribe [:get saved-data-db-path]))]
          [:<>
           [:button.pure-button {:type "button"
                                 :class (when disabled? "disabled")
                                 :on-click (if disabled?
                                             #(js/alert "Save your changes first")
                                             #(generate-svg-clicked form-db-path))
                                 :style {:float "left"}}
            "SVG Link"]
           [:button.pure-button {:type "button"
                                 :class (when disabled? "disabled")
                                 :on-click (if disabled?
                                             #(js/alert "Save your changes first")
                                             #(generate-png-clicked form-db-path))
                                 :style {:float "left"
                                         :margin-left "5px"}}
            "PNG Link"]])
        [:button.pure-button.pure-button-primary {:type "submit"}
         "Save"]]]
      [component/form-render-options (conj form-db-path :render-options)]
      [component/form-for-coat-of-arms (conj form-db-path :coat-of-arms)]]]))

(defn arms-display []
  (let [user-data (user/data)
        arms-data @(rf/subscribe [:get form-db-path])
        arms-id (-> arms-data
                    :id
                    id-for-url)]
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
       [:button.pure-button {:type "button"
                             :on-click #(generate-svg-clicked form-db-path)
                             :style {:float "left"}}
        "SVG Link"]
       [:button.pure-button {:type "button"
                             :on-click #(generate-png-clicked form-db-path)
                             :style {:float "left"
                                     :margin-left "5px"}}
        "PNG Link"]
       (when (= (:username arms-data)
                (:username user-data))
         [:button.pure-button.pure-button-primary {:type "button"
                                                   :on-click #(do
                                                                (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                (reife/push-state :edit-arms-by-id {:id arms-id}))}
          "Edit"])]
      [component/form-render-options (conj form-db-path :render-options)]
      [component/form-for-coat-of-arms (conj form-db-path :coat-of-arms)]]]))

(defn list-arms-for-user [user-id]
  (let [arms-list @(rf/subscribe [:get list-db-path])]
    (cond
      (nil? arms-list) (do
                         (fetch-arms-list-by-user user-id)
                         [:<>])
      (= arms-list :loading) [:<>]
      :else (if (empty? arms-list)
              [:span "None"]
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
                                       (rf/dispatch-sync [:set form-db-path nil])
                                       (rf/dispatch-sync [:clear-form-errors form-db-path])
                                       (reife/href :view-arms-by-id {:id arms-id}))}
                      (:name arms)])]))]))))

(defn list-my-arms []
  (let [user-data (user/data)]
    [:div {:style {:padding "15px"}}
     [:h4 "My arms"]
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:set form-db-path nil])
                    (rf/dispatch-sync [:clear-form-errors form-db-path])
                    (reife/push-state :create-arms))}
      "Create"]
     [list-arms-for-user (:user-id user-data)]]))

(defn create-arms []
  (let [form-data @(rf/subscribe [:get form-db-path])]
    (when (nil? form-data)
      (rf/dispatch [:set form-db-path {:coat-of-arms default/coat-of-arms
                                       :render-options {:component :render-options
                                                        :mode :colours
                                                        :outline? false
                                                        :squiggly? false
                                                        :ui {:selectable-fields? true}}}])))
  [arms-form])

(defn edit-arms [arms-id version]
  (let [arms-form-data @(rf/subscribe [:get form-db-path])]
    (cond
      (and arms-id
           (nil? arms-form-data)) (do
                                    (fetch-arms-and-fill-form arms-id version)
                                    [:<>])
      (= arms-form-data :loading) [:<>]
      arms-form-data [arms-form]
      :else [:<>])))

(defn view-arms [arms-id version]
  (let [arms-form-data @(rf/subscribe [:get form-db-path])]
    (cond
      (and arms-id
           (nil? arms-form-data)) (do
                                    (fetch-arms-and-fill-form arms-id version)
                                    [:<>])
      (= arms-form-data :loading) [:<>]
      arms-form-data [arms-display]
      :else [:<>])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn view-list-arms []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [list-my-arms]
      [not-logged-in])))

(defn edit-arms-by-id [{:keys [parameters]}]
  (let [user-data (user/data)
        id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    (if (:logged-in? user-data)
      [edit-arms arms-id version]
      [not-logged-in])))

(defn view-arms-by-id [{:keys [parameters]}]
  (let [user-data (user/data)
        id (-> parameters :path :id)
        version (-> parameters :path :version)
        arms-id (str "arms:" id)]
    (if (:logged-in? user-data)
      [view-arms arms-id version]
      [not-logged-in])))
