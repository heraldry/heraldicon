(ns heraldry.frontend.collection-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.config :as config]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.context :as context]
            [heraldry.frontend.credits :as credits]
            [heraldry.frontend.form.arms-select :as arms-select]
            [heraldry.frontend.form.attribution :as attribution]
            [heraldry.frontend.form.collection :as collection]
            [heraldry.frontend.form.collection-select :as collection-select]
            [heraldry.frontend.form.core :as form]
            [heraldry.frontend.form.font :as font]
            [heraldry.frontend.form.render-options :as render-options]
            [heraldry.frontend.form.tag :as tag]
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

(def selected-arms-path
  [:ui :collection :selected-arms])

(defn fetch-collection [collection-id version]
  (go
    (try
      (let [user-data (user/data)
            full-data (<? (api-request/call :fetch-collection {:id collection-id
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
      (let [payload @(rf/subscribe [:get form-db-path])
            user-data (user/data)
            response (<? (api-request/call :save-collection payload user-data))
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

(defn render-add-arms [x y size]
  (let [r (* size 0.4)
        bar-width (* size 0.5)
        bar-height (* size 0.1)]
    [:g {:transform (str "translate(" x "," y ")")}
     [:circle
      {:r r
       :style {:fill "#ccc"
               :stroke-width 0.1
               :stroke "#000"}}]
     [:rect
      {:x (/ bar-width -2)
       :y (/ bar-height -2)
       :width bar-width
       :height bar-height
       :style {:fill "#fff"}}]
     [:rect
      {:x (/ bar-height -2)
       :y (/ bar-width -2)
       :width bar-height
       :height bar-width
       :style {:fill "#fff"}}]]))

(defn render-arms [x y size path render-options & {:keys [selected? font font-size]
                                                   :or {font-size 12}}]
  (let [data @(rf/subscribe [:get path])
        {arms-id :id
         version :version} (:reference data)
        [_status arms-data] (when arms-id
                              (state/async-fetch-data
                               [:arms-references arms-id version]
                               [arms-id version]
                               #(arms-select/fetch-arms arms-id version nil)))
        {:keys [result
                environment]} (render/coat-of-arms
                               (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                 coat-of-arms
                                 default/coat-of-arms)
                               size
                               (merge
                                context/default
                                {:render-options render-options
                                 :db-path []
                                 :fn-select-component nil
                                 #_#_:metadata [metadata/attribution name username (full-url-for-username username) arms-url attribution]}))
        {:keys [width height]} environment]
    [:g
     (when selected?
       [:rect {:x (- x (/ width 2) 7)
               :y (- y (/ height 2) 7)
               :rx 10
               :width (+ width 14)
               :height (+ height 14)
               :fill "#33f8"}])
     [:g {:transform (str "translate(" (- x (/ width 2)) "," (- y (/ height 2)) ")")}
      [:g {:filter (when (:escutcheon-shadow? render-options)
                     "url(#shadow)")}
       result]
      [:text {:x (/ width 2)
              :y (+ height 10 font-size)
              :text-anchor "middle"
              :style {:font-family font
                      :font-size font-size}}
       (:name data)]]]))

(defn on-arms-click [event index]
  (rf/dispatch [:set selected-arms-path index])
  (rf/dispatch [:ui-submenu-open (conj form-db-path :collection :elements index)])
  (state/dispatch-on-event event [:ui-component-open (conj form-db-path :collection :elements index)]))

(defn render-collection [& {:keys [allow-adding?]}]
  (let [render-options @(rf/subscribe [:get (conj form-db-path :render-options)])
        selected-arms @(rf/subscribe [:get selected-arms-path])
        collection-data @(rf/subscribe [:get form-db-path])
        font (-> collection-data :font font/css-string)
        collection (:collection collection-data)
        {:keys [num-columns
                elements]} collection
        ;; TODO: ugly dependency, should go through the options system to sanitize and provide a default
        num-columns (or num-columns 6)
        num-elements (count elements)
        num-rows (inc (quot num-elements
                            num-columns))
        margin 10
        arms-width 100
        roll-width (+ (* num-columns
                         arms-width)
                      (* (inc num-columns)
                         margin))
        arms-height (* 1.6 arms-width)
        roll-height (+ 60
                       (* num-rows
                          arms-height)
                       (* (inc num-rows)
                          margin))]
    (if collection-data
      [:div {:style {:margin-left  "10px"
                     :margin-right "10px"}}
       [:svg {:id "svg"
              :style {:width "100%"}
              :viewBox (str "0 0 " roll-width " " roll-height)
              :preserveAspectRatio "xMidYMin slice"
              :on-click #(rf/dispatch [:set selected-arms-path nil])}
        [:g
         [:text {:x (/ roll-width 2)
                 :y 50
                 :text-anchor "middle"
                 :style {:font-family font
                         :font-size 50}}
          (:name collection-data)]]
        [:g {:transform "translate(0,60)"}
         (doall
          (for [[idx _arms] (map-indexed vector elements)]
            (let [x (mod idx num-columns)
                  y (quot idx num-columns)]
              ^{:key idx}
              [:g {:on-click #(on-arms-click % idx)
                   :style {:cursor "pointer"}}
               [render-arms
                (+ margin
                   (* x (+ arms-width
                           margin))
                   (+ (/ arms-width 2)))
                (+ margin
                   (* y (+ arms-height
                           margin))
                   (+ (/ arms-height 2)))
                arms-width
                (conj form-db-path :collection :elements idx)
                render-options
                :selected? (= idx selected-arms)
                :font font]])))

         (when allow-adding?
           (let [x (mod num-elements num-columns)
                 y (quot num-elements num-columns)]
             ^{:key num-elements}
             [:g {:on-click #(do
                               (rf/dispatch [:add-arms-to-collection form-db-path {}])
                               (state/dispatch-on-event % [:set selected-arms-path num-elements]))
                  :style {:cursor "pointer"}}
              [render-add-arms
               (+ margin
                  (* x (+ arms-width
                          margin))
                  (+ (/ arms-width 2)))
               (+ margin
                  (* y (+ arms-height
                          margin))
                  (+ (/ arms-height 2)))
               arms-width]]))]]]
      [:div "loading..."])))

(defn render-arms-preview [arms-reference]
  (let [render-options @(rf/subscribe [:get (conj form-db-path :render-options)])
        {arms-id :id
         version :version} arms-reference
        [status arms-data] (when arms-id
                             (state/async-fetch-data
                              [:arms-references arms-id version]
                              [arms-id version]
                              #(arms-select/fetch-arms arms-id version nil)))
        {:keys [result
                environment]} (render/coat-of-arms
                               (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                 coat-of-arms
                                 default/coat-of-arms)
                               100
                               (merge
                                context/default
                                {:render-options render-options
                                 :db-path []
                                 :fn-select-component nil
                                 #_#_:metadata [metadata/attribution name username (full-url-for-username username) arms-url attribution]}))
        {:keys [width height]} environment]
    (when (= status :done)
      [:div
       [:div.credits {:style {:margin-bottom 0}}
        [credits/for-arms arms-data]]

       [:svg {:id "svg"
              :style {:width "100%"}
              :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
              :preserveAspectRatio "xMidYMin slice"}
        [:g {:filter (when (:escutcheon-shadow? render-options)
                       "url(#shadow)")}
         [:g {:transform "translate(10,10) scale(5,5)"}
          result]]]])))

(defn collection-form []
  (let [selected-arms @(rf/subscribe [:get selected-arms-path])
        collection-data @(rf/subscribe [:get form-db-path])
        error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        on-submit (fn [event]
                    (.preventDefault event)
                    (.stopPropagation event)
                    (save-collection-clicked))
        logged-in? (-> (user/data)
                       :logged-in?)]
    [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                 (rf/dispatch [:ui-submenu-close-all])
                                 (.stopPropagation %))}
     [:div.pure-u-1-2.no-scrollbar {:style {:position "fixed"
                                            :height "100vh"
                                            :overflow-y "scroll"}}
      [render-collection
       :allow-adding? true]]
     [:div.pure-u-1-2 {:style {:margin-left "50%"
                               :width "45%"}}
      [attribution/form (conj form-db-path :attribution)]
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
       [:fieldset
        [tag/form (conj form-db-path :tags)]]
       (when form-message
         [:div.form-message form-message])
       (when error-message
         [:div.error-message error-message])
       [:div.pure-control-group {:style {:text-align "right"
                                         :margin-top "10px"}}
        [:button.pure-button.pure-button
         {:type "button"
          :on-click (let [match @(rf/subscribe [:get [:route-match]])
                          route (-> match :data :name)
                          params (-> match :parameters :path)]
                      (cond
                        (= route :edit-collection-by-id) #(reife/push-state :view-collection-by-id params)
                        (= route :create-collection) #(reife/push-state :collection params)
                        :else nil))
          :style {:margin-right "5px"}}
         "View"]
        (let [disabled? (not logged-in?)]
          [:button.pure-button.pure-button-primary {:type "submit"
                                                    :class (when disabled? "disabled")}
           "Save"])
        [:div.spacer]]]
      [render-options/form (conj form-db-path :render-options)]
      [collection/form form-db-path selected-arms-path]

      (when selected-arms
        (let [element (get-in collection-data [:collection :elements selected-arms])]
          [render-arms-preview (:reference element)]))]]))

(defn collection-display [collection-id version]
  (let [user-data (user/data)
        selected-arms @(rf/subscribe [:get selected-arms-path])
        [status collection-data] (state/async-fetch-data
                                  form-db-path
                                  [collection-id version]
                                  #(fetch-collection collection-id version))
        collection-id (-> collection-data
                          :id
                          id-for-url)]
    (when (= status :done)
      [:div.pure-g {:on-click #(do (rf/dispatch [:ui-component-deselect-all])
                                   (rf/dispatch [:ui-submenu-close-all])
                                   (.stopPropagation %))}
       [:div.pure-u-1-2.no-scrollbar {:style {:position "fixed"
                                              :height "100vh"
                                              :overflow-y "scroll"}}
        [render-collection]]
       [:div.pure-u-1-2 {:style {:margin-left "50%"
                                 :width "45%"}}
        [:div.credits
         [credits/for-collection collection-data]]
        [:div.pure-control-group {:style {:text-align "right"
                                          :margin-top "10px"
                                          :margin-bottom "10px"}}

         (when (or (= (:username collection-data)
                      (:username user-data))
                   ((config/get :admins) (:username user-data)))
           [:button.pure-button.pure-button-primary {:type "button"
                                                     :on-click #(do
                                                                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                                                                  (rf/dispatch-sync [:clear-form-message form-db-path])
                                                                  (reife/push-state :edit-collection-by-id {:id collection-id}))}
            "Edit"])
         [:div.spacer]]
        [render-options/form (conj form-db-path :render-options)]

        (when selected-arms
          (let [element (get-in collection-data [:collection :elements selected-arms])]
            [render-arms-preview (:reference element)]))]])))

(defn link-to-collection [collection]
  (let [collection-id (-> collection
                          :id
                          id-for-url)]
    [:a {:href     (reife/href :view-collection-by-id {:id collection-id})
         :on-click #(do
                      (rf/dispatch-sync [:clear-form-errors form-db-path])
                      (rf/dispatch-sync [:clear-form-message form-db-path]))}
     (:name collection)]))

(defn list-collections []
  [collection-select/list-collections link-to-collection])

(defn invalidate-collection-cache [user-id]
  (state/invalidate-cache list-db-path user-id))

(defn view-list-collection []
  [:div {:style {:padding "15px"}}
   [:div.pure-u-1-2 {:style {:display "block"
                             :text-align "justify"
                             :min-width "30em"}}
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
   [:div {:style {:padding-top "0.5em"}}
    [list-collections]]])

(defn create-collection [match]
  (rf/dispatch [:set [:route-match] match])
  (let [[status _collection-form-data] (state/async-fetch-data
                                        form-db-path
                                        :new
                                        #(go
                                           {:num-columns 6
                                            :elements []
                                            :render-options (-> default/render-options
                                                                (dissoc :escutcheon-shadow?)
                                                                (assoc :escutcheon-outline? true))}))]
    (when (= status :done)
      [collection-form])))

(defn edit-collection [collection-id version]
  (let [[status _collection-form-data] (state/async-fetch-data
                                        form-db-path
                                        [collection-id version]
                                        #(fetch-collection collection-id version))]
    (when (= status :done)
      [collection-form])))

(defn edit-collection-by-id [{:keys [parameters] :as match}]
  (rf/dispatch [:set [:route-match] match])
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        collection-id (str "collection:" id)]
    [edit-collection collection-id version]))

(defn view-collection-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        collection-id (str "collection:" id)]
    [collection-display collection-id version]))
