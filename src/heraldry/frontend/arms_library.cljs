(ns heraldry.frontend.arms-library
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [go <!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs.reader :as reader]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<? go-catch]]
            [heraldry.api.request :as api-request]
            [heraldry.frontend.form.component :as component]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.user :as user]
            [heraldry.coat-of-arms.blazon :as blazon]
            [heraldry.coat-of-arms.config :as config]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.hatching :as hatching]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary :as ordinary]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.coat-of-arms.tincture :as tincture]
            [re-frame.core :as rf]))

;; functions


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

(defn arms-path [arms-id]
  (str "/arms/#" arms-id))

;; views

(defn render-coat-of-arms []
  (let [coat-of-arms @(rf/subscribe [:get [:arms-form :coat-of-arms]])
        render-options @(rf/subscribe [:get [:arms-form :render-options]])]
    (if coat-of-arms
      [:div {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                            (rf/dispatch [:ui-submenu-close-all])
                            (.stopPropagation %))
             :style {:margin-left "10px"
                     :margin-right "10px"}}
       [:svg {:id "svg"
              :style {:width "25em"
                      :height "32em"}
              :viewBox "0 0 520 1000"
              :preserveAspectRatio "xMidYMin slice"}
        [:defs
         filter/shadow
         filter/shiny
         filter/glow
         tincture/patterns
         hatching/patterns]
        [:g {:filter "url(#shadow)"}
         [:g {:transform "translate(10,10) scale(5,5)"}
          [render/coat-of-arms coat-of-arms render-options :db-path [:arms-form :coat-of-arms]]]]]
       [:div.blazonry
        [:span.disclaimer "Blazon (very rudimentary, very beta)"]
        [:div.blazon
         (blazon/encode-field (:field coat-of-arms) :root? true)]]]
      [:<>])))

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
            (state/goto (arms-path arms-id))))
        (catch :default e
          (println "save-form error:" e))))))

(defn arms-form []
  (let [db-path [:arms-form]
        error-message @(rf/subscribe [:get-form-error db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (save-arms-clicked db-path))]
    [:div.pure-g
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
      {:on-click #(rf/dispatch [:set [:arms-form] {:coat-of-arms config/default-coat-of-arms
                                                   :render-options {:component :render-options
                                                                    :mode :colours
                                                                    :outline? false
                                                                    :squiggly? false
                                                                    :ui {:selectable-fields? true}}}])}
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
                  (let [href (str (state/path) "#" (:id arms))]
                    [:a {:href href
                         :on-click #(do
                                      (.preventDefault %)
                                      (state/goto href))}
                     (:name arms) " "
                     [:i.far.fa-edit]])]))])]))

(defn logged-in []
  (let [arms-form-data @(rf/subscribe [:get [:arms-form]])
        path-extra (state/path-extra)]
    (cond
      (and path-extra
           (nil? arms-form-data)) (do
                                    (fetch-arms-and-fill-form path-extra)
                                    [:<>])
      (= arms-form-data :loading) [:<>]
      arms-form-data [arms-form]
      :else [list-arms-for-user])))

(defn not-logged-in []
  [:div {:style {:padding "15px"}}
   "You need to be logged in."])

(defn main []
  (let [user-data (user/data)]
    (if (:logged-in? user-data)
      [logged-in]
      [not-logged-in])))
