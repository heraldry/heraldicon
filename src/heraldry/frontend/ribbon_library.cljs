(ns heraldry.frontend.ribbon-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.attribution :as attribution]
            #_[heraldry.frontend.ribbon :as ribbon]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            #_[heraldry.frontend.ui.element.ribbon-select :as ribbon-select]
            [heraldry.frontend.ui.shared :as shared]
            [heraldry.frontend.user :as user]
            [heraldry.render :as render]
            [heraldry.util :refer [id-for-url]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]
            [taoensso.timbre :as log]))

(def form-db-path
  [:ribbon-form])

(def list-db-path
  [:ribbon-list])

(def example-coa-db-path
  [:example-coa])

;; views

(defn preview []
  (let [{:keys [data]
         :as form-data} @(rf/subscribe [:get-value form-db-path])
        {:keys [edn-data]} data
        prepared-ribbon-data (-> form-data
                                 (assoc :data edn-data)
                                 (update :username #(or % (:username (user/data)))))
        coat-of-arms @(rf/subscribe [:get-value (conj example-coa-db-path :coat-of-arms)])
        {:keys [result
                environment]} (render/coat-of-arms
                               [:context :coat-of-arms]
                               100
                               (-> shared/coa-select-option-context
                                   (assoc :root-transform "scale(5,5)")
                                   (assoc :render-options-path
                                          (conj example-coa-db-path :render-options))
                                   (assoc :coat-of-arms
                                          (-> coat-of-arms
                                              (assoc-in [:field :components 0 :data] prepared-ribbon-data)))))
        {:keys [width height]} environment]
    [:svg {:viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}}
     [:g {:transform "translate(10,10)"}
      result]]))

(defn invalidate-ribbons-cache []
  (let [user-data (user/data)
        user-id (:user-id user-data)]
    (rf/dispatch-sync [:set list-db-path nil])
    (state/invalidate-cache list-db-path user-id)
    (state/invalidate-cache [:all-ribbons] :all-ribbons)))

(defn save-ribbon-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [payload @(rf/subscribe [:get-value form-db-path])
        user-data (user/data)]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (go
      (try
        (modal/start-loading)
        (let [response (<? (api-request/call :save-ribbon payload user-data))
              ribbon-id (-> response :ribbon-id)]
          (rf/dispatch-sync [:set (conj form-db-path :id) ribbon-id])
          (state/invalidate-cache-without-current form-db-path [ribbon-id nil])
          (state/invalidate-cache-without-current form-db-path [ribbon-id 0])
          (invalidate-ribbons-cache)
          (rf/dispatch-sync [:set-form-message form-db-path (str "Ribbon saved, new version: " (:version response))])
          (reife/push-state :view-ribbon-by-id {:id (id-for-url ribbon-id)}))
        (modal/stop-loading)
        (catch :default e
          (log/error "save-form error:" e)
          (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
          (modal/stop-loading))))))

(defn copy-to-new-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [ribbon-data @(rf/subscribe [:get-value form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (state/set-async-fetch-data
     form-db-path
     :new
     (-> ribbon-data
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
    (reife/push-state :create-ribbon)))

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        ribbon-id @(rf/subscribe [:get-value (conj form-db-path :id)])
        ribbon-username @(rf/subscribe [:get-value (conj form-db-path :username)])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        saved? ribbon-id
        owned-by-me? (= (:username user-data) ribbon-username)
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

     [:div.buttons {:style {:display "flex"}}
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
      [:button.button.primary
       {:type "submit"
        :class (when-not can-save? "disabled")
        :on-click (if can-save?
                    save-ribbon-clicked
                    #(js/alert "Need to be logged in and own the arms."))
        :style {:flex "initial"
                :margin-left "10px"}}
       "Save"]]]))

(defn attribution []
  (let [attribution-data (attribution/for-ribbon form-db-path {})]
    [:div.attribution
     [:h3 "Attribution"]
     [:div {:style {:padding-left "1em"}}
      attribution-data]]))

(defn ribbon-form []
  (if @(rf/subscribe [:get-value (conj form-db-path :id)])
    (rf/dispatch [:set-title @(rf/subscribe [:get-value (conj form-db-path :name)])])
    (rf/dispatch [:set-title "Create Ribbon"]))
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path
                                                                     example-coa-db-path]])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(26em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"}}
    [preview]]
   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"}}
    [ui/selected-component]
    [button-row]
    [attribution]]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"}}
    [ui/component-tree [form-db-path
                        (conj example-coa-db-path :render-options)
                        (conj example-coa-db-path :coat-of-arms :field :components 0)]]]])

(defn ribbon-display [ribbon-id version]
  (let [[status ribbon-data] nil #_(state/async-fetch-data
                                    form-db-path
                                    [ribbon-id version]
                                    #(ribbon/fetch-ribbon-for-editing ribbon-id version))]
    (when (= status :done)
      (if ribbon-data
        [ribbon-form]
        [:div "Not found"]))))

(defn link-to-ribbon [ribbon & {:keys [type-prefix?]}]
  (let [ribbon-id (-> ribbon
                      :id
                      id-for-url)]
    [:a {:href (reife/href :view-ribbon-by-id {:id ribbon-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (when type-prefix?
       (str (-> ribbon :type name) ": "))
     (:name ribbon)]))

(defn create-ribbon [_match]
  (let [[status _ribbon-form-data] (state/async-fetch-data
                                    form-db-path
                                    :new
                                    #(go
                                       ;; TODO: make a default ribbon here?
                                       {}))]
    (when (= status :done)
      [ribbon-form])))

(defn view-list-ribbons []
  (rf/dispatch [:set-title "Ribbons"])
  (let [[status ribbons] nil #_(state/async-fetch-data
                                [:all-ribbons]
                                :all-ribbons
                                ribbon/fetch-ribbons)]
    [:div {:style {:padding "15px"}}
     [:div {:style {:text-align "justify"
                    :max-width "40em"}}
      [:p
       "Here you can view and create ribbons to be used in coats of arms. By default your ribbons "
       "are private, so only you can see and use them. If you want to make them public, then you "
       [:b "must"] " provide a license and attribution, if it is based on previous work."]]
     [:button.button.primary
      {:on-click #(do
                    (rf/dispatch-sync [:clear-form-errors form-db-path])
                    (rf/dispatch-sync [:clear-form-message form-db-path])
                    (reife/push-state :create-ribbon))}
      "Create"]
     [:div {:style {:padding-top "0.5em"}}
      "TBD"
      #_(if (= status :done)
          [ribbon-select/component ribbons link-to-ribbon invalidate-ribbons-cache]
          [:div "loading..."])]]))

(defn view-ribbon-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        ribbon-id (str "ribbon:" id)]
    [ribbon-display ribbon-id version]))

