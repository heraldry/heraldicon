(ns heraldry.frontend.collection-library
  (:require
   [cljs.core.async :refer [go]]
   [com.wsscode.common.async-cljs :refer [<?]]
   [heraldry.coat-of-arms.default :as default]
   [heraldry.context :as c]
   [heraldry.font :as font]
   [heraldry.frontend.api.request :as api-request]
   [heraldry.frontend.attribution :as attribution]
   [heraldry.frontend.history.core :as history]
   [heraldry.frontend.language :refer [tr]]
   [heraldry.frontend.state :as state]
   [heraldry.frontend.ui.core :as ui]
   [heraldry.frontend.ui.element.arms-select :as arms-select]
   [heraldry.frontend.ui.element.collection-select :as collection-select]
   [heraldry.frontend.ui.form.collection-element :as collection-element]
   [heraldry.frontend.ui.shared :as shared]
   [heraldry.frontend.user :as user]
   [heraldry.render :as render]
   [heraldry.strings :as strings]
   [heraldry.util :as util :refer [id-for-url]]
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
        (rf/dispatch-sync [:set-form-message form-db-path
                           (util/str-tr {:en "Collection saved, new version: "
                                         :de "Sammlung gespeichert, neue Version: "} (:version response))])
        (reife/push-state :view-collection-by-id {:id (id-for-url collection-id)}))
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

(defn selected-element-index []
  (let [selected-node-path @(rf/subscribe [:collection-library-highlighted-element])
        index (last selected-node-path)]
    (when (int? index)
      (if (< index @(rf/subscribe [:get-list-size (conj form-db-path :collection :elements)]))
        index
        ;; index not valid anymore
        (do
          (collection-element/highlight-element nil)
          nil)))))

(defn arms-highlight [path x y width height]
  (if @(rf/subscribe [:collection-library-highlighted? path])
    [:rect {:x (- x (/ width 2) 7)
            :y (- y (/ height 2) 7)
            :rx 10
            :width (+ width 14)
            :height (+ height 14)
            :fill "#33f8"}]
    [:<>]))

(defn render-arms [x y size path & {:keys [font font-size]
                                    :or {font-size 12}}]
  (let [data @(rf/subscribe [:get path])
        {arms-id :id
         version :version} (:reference data)
        [_status arms-data] (when arms-id
                              (state/async-fetch-data
                               [:arms-references arms-id version]
                               [arms-id version]
                               #(arms-select/fetch-arms arms-id version nil)))
        collection-render-options @(rf/subscribe [:heraldry.state/sanitized-data {:path (conj form-db-path :render-options)}])
        {:keys [result
                environment]} (render/coat-of-arms
                               (-> shared/coa-select-option-context
                                   (c/<< :path [:context :coat-of-arms])
                                   (c/<< :render-options-path [:context :render-options])
                                   (c/<< :render-options (-> arms-data
                                                             :render-options
                                                             (merge (cond-> collection-render-options
                                                                      (-> collection-render-options
                                                                          :escutcheon (= :none)) (dissoc :escutcheon)))))
                                   (c/<< :coat-of-arms
                                         (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                           coat-of-arms
                                           default/coat-of-arms)))
                               size)

        {:keys [width height]} environment]
    [:g
     [arms-highlight path x y width height]
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

(defn render-collection [& {:keys [allow-adding?]}]
  (let [font (some-> @(rf/subscribe [:get (conj form-db-path :font)])
                     font/css-string)
        num-columns @(rf/subscribe [:heraldry.state/sanitized-data {:path (conj form-db-path :collection :num-columns)}])
        num-elements @(rf/subscribe [:get-list-size (conj form-db-path :collection :elements)])
        name @(rf/subscribe [:get (conj form-db-path :name)])
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
       name]]
     [:g {:transform "translate(0,60)"}
      (doall
       (for [idx (range num-elements)]
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
             :font font]])))

      (when allow-adding?
        (let [x (mod num-elements num-columns)
              y (quot num-elements num-columns)]
          ^{:key num-elements}
          [:g {:on-click #(state/dispatch-on-event % [:add-element {:path (conj form-db-path :collection :elements)} {}])
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
            arms-width]]))]]))

(defn render-arms-preview []
  (when-let [selected-element-index (selected-element-index)]
    (let [arms-reference @(rf/subscribe [:get (conj form-db-path :collection :elements selected-element-index :reference)])
          {arms-id :id
           version :version} arms-reference
          [status arms-data] (when arms-id
                               (state/async-fetch-data
                                [:arms-references arms-id version]
                                [arms-id version]
                                #(arms-select/fetch-arms arms-id version nil)))
          collection-render-options @(rf/subscribe [:heraldry.state/sanitized-data {:path (conj form-db-path :render-options)}])]
      (when (or (not arms-id)
                (= status :done))
        (let [{:keys [result
                      environment]} (render/coat-of-arms
                                     (-> shared/coa-select-option-context
                                         (assoc :path [:context :coat-of-arms])
                                         (assoc :render-options-path [:context :render-options])
                                         (assoc :render-options (-> arms-data
                                                                    :render-options
                                                                    (merge (cond-> collection-render-options
                                                                             (-> collection-render-options
                                                                                 :escutcheon (= :none)) (dissoc :escutcheon)))))
                                         (assoc :coat-of-arms
                                                (if-let [coat-of-arms (:coat-of-arms arms-data)]
                                                  coat-of-arms
                                                  default/coat-of-arms)))
                                     100)
              {:keys [width height]} environment]
          [:<>
           (when arms-id
             [:div.attribution
              [attribution/for-arms {:path [:context :arms]
                                     :arms arms-data}]])
           [:svg {:id "svg"
                  :style {:width "100%"}
                  :viewBox (str "0 0 " (-> width (* 5) (+ 20)) " " (-> height (* 5) (+ 20) (+ 20)))
                  :preserveAspectRatio "xMidYMin meet"}
            [:g {:transform "translate(10,10) scale(5,5)"}
             result]]])))))

(defn button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        collection-id @(rf/subscribe [:get (conj form-db-path :id)])
        collection-username @(rf/subscribe [:get (conj form-db-path :username)])
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        saved? collection-id
        owned-by-me? (= (:username user-data) collection-username)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))]
    [:<>
     (when form-message
       [:div.success-message [tr form-message]])
     (when error-message
       [:div.error-message error-message])

     [:div.buttons {:style {:display "flex"}}
      [:div {:style {:flex "auto"}}]
      [:button.button.primary {:type "submit"
                               :class (when-not can-save? "disabled")
                               :on-click (if can-save?
                                           save-collection-clicked
                                           #(js/alert (tr {:en "Need to be logged in and own the collection."
                                                           :de "Du mußt eingeloggt und der Besitzer der Sammlung sein."})))
                               :style {:flex "initial"
                                       :margin-left "10px"}}
       [tr strings/save]]]]))

(defn collection-form []
  (if @(rf/subscribe [:get (conj form-db-path :id)])
    (rf/dispatch [:set-title @(rf/subscribe [:get (conj form-db-path :name)])])
    (rf/dispatch [:set-title [tr {:en "Create Collection"
                                  :de "Neue Sammlung"}]]))
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(25em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"
                               :padding-left "5px"}}
    [render-collection :allow-adding? true]]
   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"}}
    [ui/selected-component]
    [button-row]
    [render-arms-preview]]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"
                               :position "relative"}}
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path
                        (conj form-db-path :render-options)
                        (conj form-db-path :collection)]]]])

(defn collection-display [collection-id version]
  (when @(rf/subscribe [:heraldry.frontend.history.core/identifier-changed? form-db-path collection-id])
    (rf/dispatch-sync [:heraldry.frontend.history.core/clear form-db-path collection-id]))
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
  (rf/dispatch [:set-title strings/collections])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [tr {:en [:p
              "Here you can view and create collections of coats of arms. "
              "You explicitly have to save your collection as "
              [:b "public"] " and add a license, if you want to share the link and allow others to view it."]
         :de [:p
              "Hier kannst du Sammlungen von Wappen erstellen und ansehen. "
              "Du mußt deine Sammlungen explizit als "
              [:b "öffentlich"] " markieren eine Lizenz angeben, wenn du anderen den Zugriff erlauben und den Link teilen möchtest."]}]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-collection))}
    [tr strings/create]]
   [:div {:style {:padding-top "0.5em"}}
    [list-collections]]])

(defn create-collection [_match]
  (when @(rf/subscribe [:heraldry.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldry.frontend.history.core/clear form-db-path nil]))
  (let [[status collection-data] (state/async-fetch-data
                                  form-db-path
                                  :new
                                  #(go
                                     {:num-columns 6
                                      :elements []
                                      :render-options (-> default/render-options
                                                          (dissoc :escutcheon-shadow?)
                                                          (assoc :escutcheon-outline? true))}))]
    (when (= status :done)
      (if collection-data
        [collection-form]
        [:div [tr strings/not-found]]))))

(defn view-collection-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        collection-id (str "collection:" id)]
    [collection-display collection-id version]))
