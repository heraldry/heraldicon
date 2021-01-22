(ns heraldry.frontend.arms-library
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [cljs.reader :as reader]
            [heraldry.api.request :as api-request]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.user :as user]
            [heraldry.frontend.util :as util]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]))

(defn fetch-arms-list-by-user [user-id]
  (go
    (let [db-path [:arms-list]
          user-data (user/data)]
      (rf/dispatch-sync [:set db-path :loading])
      (-> (api-request/call :list-arms {:user-id user-id} user-data)
          <!
          (as-> response
                (let [error (:error response)]
                  (if error
                    (println "fetch-arms-list-by-user error:" error)
                    (rf/dispatch-sync [:set db-path (:arms response)]))))))))

(defn fetch-url-data-to-path [db-path url function]
  (go
    (-> (http/get url)
        <!
        (as-> response
              (let [status (:status response)
                    body (:body response)]
                (if (= status 200)
                  (do
                    (println "retrieved" url)
                    (rf/dispatch [:set db-path (if function
                                                 (function body)
                                                 body)]))
                  (println "error fetching" url)))))))

(defn fetch-arms-and-fill-form [arms-id]
  (go
    (let [form-db-path [:arms-form]
          user-data (user/data)]
      (rf/dispatch-sync [:set form-db-path :loading])
      (-> (api-request/call :fetch-arms {:id arms-id} user-data)
          <!
          (as-> response
                (if-let [error (:error response)]
                  (println ":fetch-arms-by-id error:" error)
                  (do
                    (rf/dispatch [:set form-db-path response])
                    (fetch-url-data-to-path (conj form-db-path :coat-of-arms)
                                            (:edn-data-url response) reader/read-string))))))))

;; views

(defn render-coat-of-arms []
  (let [coat-of-arms @(rf/subscribe [:get [:arms-form :coat-of-arms]])
        render-options @(rf/subscribe [:get [:arms-form :render-options]])]
    (if coat-of-arms
      (let [{:keys [result
                    environment]} (render/coat-of-arms
                                   coat-of-arms
                                   100
                                   (merge
                                    context/default
                                    {:render-options render-options
                                     :db-path [:arms-form :coat-of-arms]}))
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
           (blazon/encode-field (:field coat-of-arms) :root? true)]]])
      [:<>])))

(defn generate-svg-clicked [db-path]
  (let [payload @(rf/subscribe [:get db-path])
        user-data (user/data)]
    (go
      (try
        (let [response (<! (api-request/call :generate-svg-arms payload user-data))
              error (:error response)]
          (println "generate-svg-arms response" response)
          (when-not error
            (println "success")
            (js/window.open (:svg-url response))))
        (catch :default e
          (println "generate-svg-arms error:" e))))))

(defn generate-png-clicked [db-path]
  (let [payload @(rf/subscribe [:get db-path])
        user-data (user/data)]
    (go
      (try
        (let [response (<! (api-request/call :generate-png-arms payload user-data))
              error (:error response)]
          (println "generate-png-arms response" response)
          (when-not error
            (println "success")
            (js/window.open (:png-url response))))
        (catch :default e
          (println "generate-png-arms error:" e))))))

(defn save-arms-clicked [db-path]
  (let [payload @(rf/subscribe [:get db-path])
        user-data (user/data)]
    (go
      (try
        (let [response (<! (api-request/call :save-arms payload user-data))
              error (:error response)
              arms-id (-> response :arms-id)]
          (println "save arms response" response)
          (when-not error
            (rf/dispatch [:set (conj db-path :id) arms-id])
            (reife/push-state :arms-by-id {:id (util/id-for-url arms-id)})))
        (catch :default e
          (println "save-form error:" e))))))

(defn arms-form []
  (let [db-path [:arms-form]
        error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (save-arms-clicked db-path))]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2 {:style {:position "fixed"}}
      [render-coat-of-arms]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width "45%"}}
      [:form.pure-form.pure-form-aligned
       {:style {:display "inline-block"}
        :on-key-press (fn [event]
                        (when (-> event .-code (= "Enter"))
                          (on-submit event)))
        :on-submit on-submit}
       (when error-message
         [:div.error-message error-message])
       [:fieldset
        [form/field (conj db-path :name)
         (fn [& {:keys [value on-change]}]
           [:div.pure-control-group
            [:label {:for "name"
                     :style {:width "6em"}} "Name"]
            [:input {:id "name"
                     :value value
                     :on-change on-change
                     :type "text"
                     :style {:margin-right "0.5em"}}]
            [form/checkbox (conj db-path :is-public) "Make public"
             :style {:width "7em"}]])]]
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button {:type "button"
                              :on-click #(generate-svg-clicked db-path)
                              :style {:float "left"}}
         "SVG Link"]
        [:button.pure-button {:type "button"
                              :on-click #(generate-png-clicked db-path)
                              :style {:float "left"
                                      :margin-left "5px"}}
         "PNG Link"]
        [:button.pure-button.pure-button-primary {:type "submit"}
         "Save"]]]
      [component/form-render-options [:arms-form]]
      [component/form-for-field [:arms-form :coat-of-arms :field]]]]))

(defn list-arms-for-user []
  (let [user-data (user/data)
        arms-list @(rf/subscribe [:get [:arms-list]])]
    [:div {:style {:padding "15px"}}
     [:h4 "My arms"]
     [:button.pure-button.pure-button-primary
      {:on-click #(do
                    (rf/dispatch-sync [:set [:arms-form] nil])
                    (reife/push-state :create-arms))}
      "Create"]
     (cond
       (nil? arms-list) (do
                          (fetch-arms-list-by-user (:user-id user-data))
                          [:<>])
       (= arms-list :loading) [:<>]
       :else [:ul.arms-list
              (doall
               (for [arms arms-list]
                 ^{:key (:id arms)}
                 [:li.arms
                  (let [arms-id (-> arms
                                    :id
                                    util/id-for-url)]
                    [:a {:href (reife/href :arms-by-id {:id arms-id})
                         :on-click #(do
                                      (rf/dispatch-sync [:set [:arms-form] nil])
                                      (reife/href :arms-by-id {:id arms-id}))}
                     (:name arms) " "
                     [:i.far.fa-edit]])]))])]))

(defn create-arms []
  (let [db-path [:arms-form]
        form-data @(rf/subscribe [:get db-path])]
    (when (nil? form-data)
      (rf/dispatch [:set [:arms-form] {:coat-of-arms default/coat-of-arms
                                       :render-options {:component :render-options
                                                        :mode :colours
                                                        :outline? false
                                                        :squiggly? false
                                                        :ui {:selectable-fields? true}}}])))
  [arms-form])

(defn arms-by-id [arms-id]
  (let [arms-form-data @(rf/subscribe [:get [:arms-form]])]
    (cond
      (and arms-id
           (nil? arms-form-data)) (do
                                    (fetch-arms-and-fill-form arms-id)
                                    [:<>])
      (= arms-form-data :loading) [:<>]
      arms-form-data [arms-form]
      :else [:<>])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn view-list-arms []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [list-arms-for-user]
      [not-logged-in])))

(defn view-arms-by-id [{:keys [parameters]}]
  (let [user-data (user/data)
        arms-id (str "arms:" (-> parameters :path :id))]
    (if (:logged-in? user-data)
      [arms-by-id arms-id]
      [not-logged-in])))
