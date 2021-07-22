(ns heraldry.frontend.collection-library
  (:require [cljs.core.async :refer [go]]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.render :as render]
            [heraldry.font :as font]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.attribution :as attribution]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            [heraldry.frontend.ui.element.arms-select :as arms-select]
            [heraldry.frontend.ui.element.collection-select :as collection-select]
            [heraldry.frontend.ui.shared :as shared]
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

(defn render-arms [x y size path & {:keys [selected? font font-size]
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
                               [:context :coat-of-arms]
                               size
                               (-> shared/coa-select-option-context
                                   (assoc :render-options-path
                                          (conj form-db-path :render-options))
                                   (assoc :coat-of-arms
                                          (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                            coat-of-arms
                                            default/coat-of-arms))))

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
      result
      [:text {:x (/ width 2)
              :y (+ height 10 font-size)
              :text-anchor "middle"
              :style {:font-family font
                      :font-size font-size}}
       (:name data)]]]))

(defn on-arms-click [event index]
  (state/dispatch-on-event event [:ui-component-node-select (conj form-db-path :collection :elements index)]))

(defn selected-element-index []
  (let [selected-node-path @(rf/subscribe [:ui-component-node-selected-path])
        index (last selected-node-path)]
    (when (int? index)
      index)))

(defn render-collection [& {:keys [allow-adding?]}]
  (let [collection-data @(rf/subscribe [:get form-db-path])
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
      [:div {:style {:margin-left "10px"
                     :margin-right "10px"}}
       [:svg {:id "svg"
              :style {:width "100%"}
              :viewBox (str "0 0 " roll-width " " roll-height)
              :preserveAspectRatio "xMidYMin slice"}
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
                :selected? (= idx (selected-element-index))
                :font font]])))

         (when allow-adding?
           (let [x (mod num-elements num-columns)
                 y (quot num-elements num-columns)]
             ^{:key num-elements}
             [:g {:on-click #(state/dispatch-on-event % [:add-element (conj form-db-path :collection :elements) {}])
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

(defn render-arms-preview []
  (when-let [selected-element-index (selected-element-index)]
    (let [arms-reference @(rf/subscribe [:get-value (conj form-db-path :collection :elements selected-element-index :reference)])
          {arms-id :id
           version :version} arms-reference
          [status arms-data] (when arms-id
                               (state/async-fetch-data
                                [:arms-references arms-id version]
                                [arms-id version]
                                #(arms-select/fetch-arms arms-id version nil)))
          {:keys [result
                  environment]} (render/coat-of-arms
                                 [:context :coat-of-arms]
                                 100
                                 (-> shared/coa-select-option-context
                                     (assoc :root-transform "scale(5,5)")
                                     (assoc :render-options-path
                                            (conj form-db-path :render-options))
                                     (assoc :coat-of-arms
                                            (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                              coat-of-arms
                                              default/coat-of-arms))))
          {:keys [width height]} environment]
      (when (or (not arms-id)
                (= status :done))
        [:<>
         (when arms-id
           [:div.no-scrollbar {:style {:grid-area "attribution"
                                       :overflow-y "scroll"
                                       :margin 0}}
            [:div.attribution
             [attribution/for-arms [:context :arms] {:arms arms-data}]]])
         [:div.no-scrollbar {:style {:grid-area "arms-preview"
                                     :overflow-y "scroll"}}
          [:svg {:id "svg"
                 :style {:width "100%"
                         :height "95%"}
                 :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 30)))
                 :preserveAspectRatio "xMidYMin meet"}
           [:g {:transform "translate(10,10)"}
            result]]]]))))

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        collection-data @(rf/subscribe [:get form-db-path])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        saved? (:id collection-data)
        owned-by-me? (= (:username user-data) (:username collection-data))
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
      [:div.spacer {:style {:flex 10}}]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-collection-clicked
                                           #(js/alert "Need to be logged in and own the collection."))}
       "Save"]]]))

(defn collection-form []
  (rf/dispatch [:ui-component-node-select-default form-db-path])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] 33% [second] 25% [end]"
                 :grid-template-rows "[top] 33% [middle] 57% [bottom-edge] calc(10% - 20px) [bottom]"
                 :grid-template-areas "'arms selected-component component-tree'
                                       'arms arms-preview component-tree'
                                       'arms attribution component-tree'"
                 :padding-left "10px"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "arms"
                               :overflow-y "scroll"}}
    [render-collection :allow-adding? true]]
   [:div {:style {:grid-area "selected-component"
                  :padding-top "10px"}}
    [ui/selected-component]
    [button-row]]
   [render-arms-preview]
   [:div {:style {:grid-area "component-tree"
                  :padding-top "5px"}}
    [ui/component-tree [form-db-path
                        (conj form-db-path :render-options)
                        (conj form-db-path :collection)]]]])

(defn collection-display [collection-id version]
  (let [[status _collection-data] (state/async-fetch-data
                                   form-db-path
                                   [collection-id version]
                                   #(fetch-collection collection-id version))]
    (when (= status :done)
      [collection-form])))

(defn link-to-collection [collection]
  (let [collection-id (-> collection
                          :id
                          id-for-url)]
    [:a {:href (reife/href :view-collection-by-id {:id collection-id})
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
     "Here you can view and create collections of coats of arms. "
     "You explicitly have to save your collection as "
     [:b "public"] " and add a license, if you want to share the link and allow others to view it."]]
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
