(ns heraldicon.frontend.library.ribbon
  (:require
   ["copy-to-clipboard" :as copy-to-clipboard]
   [cljs.core.async :refer [go]]
   [com.wsscode.async.async-cljs :refer [<?]]
   [heraldicon.entity.attribution :as entity.attribution]
   [heraldicon.entity.id :as id]
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.api.request :as api.request]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.modal :as modal]
   [heraldicon.frontend.not-found :as not-found]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]
   [heraldicon.frontend.ui.element.ribbon-select :as ribbon-select]
   [heraldicon.frontend.user :as user]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.curve.bezier :as bezier]
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.vector :as v]
   [heraldicon.render.core :as render]
   [heraldicon.svg.filter :as filter]
   [heraldicon.svg.path :as path]
   [heraldicon.util.core :as util]
   [re-frame.core :as rf]
   [reitit.frontend.easy :as reife]
   [taoensso.timbre :as log]))

(def form-db-path
  [:ribbon-form])

(def ^:private saved-data-db-path
  [:saved-ribbon-data])

(def ^:private preview-width
  500)

(def ^:private preview-height
  600)

(def ^:private ribbon-min-x
  (- (/ preview-width 2)))

(def ^:private ribbon-max-x
  (/ preview-width 2))

(def ^:private ribbon-min-y
  (- (/ preview-height 2)))

(def ^:private ribbon-max-y
  (/ preview-height 2))

(def ^:private render-context
  {:render-options default/render-options
   :render-options-path [:context :render-options]})

(macros/reg-event-db ::edit-remove-point
  (fn [db [_ path]]
    (let [points-path (-> path drop-last vec)
          idx (last path)]
      (update-in db points-path
                 (fn [path]
                   (vec (concat (take idx path)
                                (drop (inc idx) path))))))))

(macros/reg-event-db ::edit-toggle-show-points
  (fn [db _]
    (update-in db [:ui :ribbon-edit :show-points?] not)))

(macros/reg-event-db ::edit-add-point
  (fn [db [_ path idx new-point]]
    (update-in db path
               (fn [points]
                 (vec (concat (take (inc idx) points)
                              [new-point]
                              (drop (inc idx) points)))))))

(defn- clamp-point [p]
  (-> p
      (update :x max ribbon-min-x)
      (update :x min ribbon-max-x)
      (update :y max ribbon-min-y)
      (update :y min ribbon-max-y)))

(rf/reg-sub ::edit-addable-points
  (fn [[_ path] _]
    (rf/subscribe [:get path]))

  (fn [points [_ _path]]
    (concat [[-1 (clamp-point (v/sub (first points)
                                     (v/Vector. 50 10)))]]

            (map-indexed (fn [idx leg]
                           [idx (bezier/interpolate-point leg 0.5)])
                         (catmullrom/catmullrom points))
            [[(dec (count points)) (clamp-point (v/add (last points)
                                                       (v/Vector. 50 10)))]])))

(rf/reg-sub ::edit-point-deletable?
  (fn [[_ path] _]
    (let [points-path (-> path drop-last vec)]
      (rf/subscribe [:get-list-size points-path])))

  (fn [num-points [_ _path]]
    (> num-points 2)))

(rf/reg-sub ::edit-key-modifiers
  (fn [db _]
    (get-in db [:ui :ribbon-edit :key-modifiers])))

(macros/reg-event-db ::edit-set-key-modifiers
  (fn [db [_ key-modifiers]]
    (assoc-in db [:ui :ribbon-edit :key-modifiers] key-modifiers)))

(rf/reg-sub ::edit-selected-point
  (fn [_ _]
    (rf/subscribe [:get [:ui :ribbon-edit :selected-point :path]]))

  (fn [selected-point-path _]
    selected-point-path))

(rf/reg-sub ::edit-point-selected?
  (fn [[_ _path] _]
    (rf/subscribe [:get [:ui :ribbon-edit :selected-point :path]]))

  (fn [selected-point-path [_ path]]
    (= selected-point-path path)))

(rf/reg-sub ::edit-show-points?
  (fn [_ _]
    (rf/subscribe [:get [:ui :ribbon-edit :show-points?]]))

  (fn [show-points? _]
    show-points?))

(rf/reg-sub ::edit-mode
  (fn [_ _]
    [(rf/subscribe [::edit-show-points?])
     (rf/subscribe [::edit-key-modifiers])])

  (fn [[show-points? {:keys [shift?]}] _]
    (if show-points?
      (cond
        shift? :add-or-remove
        :else :edit)
      :none)))

(macros/reg-event-db ::edit-select-point
  (fn [db [_ path pos]]
    (if path
      (let [current-pos (get-in db path)
            dx (- (:x pos) (:x current-pos))
            dy (- (:y pos) (:y current-pos))]
        (assoc-in db [:ui :ribbon-edit :selected-point] {:path path
                                                         :dx dx
                                                         :dy dy}))
      (assoc-in db [:ui :ribbon-edit :selected-point] nil))))

(macros/reg-event-db ::edit-point-move-selected
  (fn [db [_ pos]]
    (let [{:keys [dx dy
                  path]} (get-in db [:ui :ribbon-edit :selected-point])]
      (if path
        (assoc-in db path (clamp-point
                           (v/sub pos (v/Vector. dx dy))))
        db))))

(def ^:private preview-svg-id
  "ribbon-preview")

(defn- map-to-svg-space [x y]
  (let [svg (js/document.getElementById preview-svg-id)
        ctm (.getScreenCTM svg)]
    [(-> x
         (- (.-e ctm))
         (/ (.-a ctm)))
     (-> y
         (- (.-f ctm))
         (/ (.-d ctm)))]))

(def ^:private path-point-size
  5)

(defn- path-point [path]
  (let [edit-mode @(rf/subscribe [::edit-mode])]
    (if (= edit-mode :none)
      [:<>]
      (let [size path-point-size
            width (* size 1.1)
            height (* size 0.2)
            {:keys [x y]} (interface/get-raw-data {:path path})
            deletable? @(rf/subscribe [::edit-point-deletable? path])
            route-path-point-click-fn (when (and (= edit-mode :add-or-remove)
                                                 deletable?)
                                        #(rf/dispatch [::edit-remove-point %]))
            route-path-point-mouse-down-fn (when (= edit-mode :edit)
                                             (fn [pos]
                                               (rf/dispatch [::edit-select-point path pos])))
            selected? @(rf/subscribe [::edit-point-selected? path])]
        [:g
         {:transform (str "translate(" x "," y ")")
          :on-mouse-down (when route-path-point-mouse-down-fn
                           (fn [event]
                             (let [mx (.-clientX event)
                                   my (.-clientY event)
                                   [sx sy] (map-to-svg-space mx my)]
                               (route-path-point-mouse-down-fn (v/Vector. sx sy)))))
          :on-click (when route-path-point-click-fn
                      #(route-path-point-click-fn path))
          :style {:cursor "pointer"}}
         [:circle {:style {:fill (if selected?
                                   "#2c2"
                                   "#fff")
                           :stroke "#000"}
                   :r size}]
         (when (and (= edit-mode :add-or-remove)
                    deletable?) [:g
                                 [:rect {:x (/ width -2)
                                         :y (/ height -2)
                                         :width width
                                         :height height
                                         :style {:fill "#000"}}]])]))))

(defn- add-point [path idx {:keys [x y] :as point}]
  (let [size path-point-size
        width (* size 1.1)
        height (* size 0.2)]
    [:g
     {:transform (str "translate(" x "," y ")")
      :on-click #(rf/dispatch [::edit-add-point path idx point])
      :style {:cursor "pointer"
              :opacity 0.5}}
     [:circle {:style {:fill "#fff"
                       :stroke "#000"}
               :r size}]
     [:g
      [:rect {:x (/ width -2)
              :y (/ height -2)
              :width width
              :height height
              :style {:fill "#000"}}]
      [:rect {:x (/ height -2)
              :y (/ width -2)
              :width height
              :height width
              :style {:fill "#000"}}]]]))

(defn- render-edit-overlay [path]
  (let [edit-mode @(rf/subscribe [::edit-mode])
        edge-angle (interface/get-sanitized-data {:path (conj path :edge-angle)})
        points-path (conj path :points)
        segments-path (conj path :segments)
        points (interface/get-raw-data {:path points-path})
        {:keys [curve curves]} (ribbon/generate-curves points edge-angle)
        selected-curve-idx (->> (count curves)
                                range
                                (keep (fn [idx]
                                        (when @(rf/subscribe [:get [:ui :submenu-open? (conj segments-path idx)]])
                                          idx)))
                                first)]
    [:<>
     (when-not (= edit-mode :none)
       [:path {:d (path/curve-to-relative curve)
               :style {:stroke-width 3
                       :stroke "#aaaaaa"
                       :stroke-dasharray "4,8"
                       :stroke-linecap "round"
                       :fill "none"}}])
     (when selected-curve-idx
       [:path {:d (path/curve-to-relative (get curves selected-curve-idx))
               :style {:stroke-width 3
                       :stroke "#6688ff"
                       :stroke-linecap "round"
                       :fill "none"}}])]))

(defn- grid-lines [width height dx dy]
  (-> [:g {:style {:stroke "#bbbbbb"
                   :stroke-width 0.2
                   :fill "none"}}]
      (into (map (fn [x]
                   ^{:key [:vertical x]}
                   [:path {:d (str "M " x ",0 v" height)}]))
            (range 0 (inc width) dx))
      (into (map (fn [y]
                   ^{:key [:horizontal y]}
                   [:path {:d (str "M 0," y " h" width)}]))
            (range 0 (inc height) dy))))

(defn- preview []
  (let [[width height] [preview-width preview-height]
        ribbon-path (conj form-db-path :data :ribbon)
        points-path (conj ribbon-path :points)
        num-points (interface/get-list-size {:path points-path})
        edit-mode @(rf/subscribe [::edit-mode])
        route-path-point-mouse-up-fn (when (= edit-mode :edit)
                                       #(rf/dispatch [::edit-select-point nil]))
        route-path-point-move-fn (when (and (= edit-mode :edit)
                                            @(rf/subscribe [::edit-selected-point]))
                                   (fn [pos]
                                     (rf/dispatch [::edit-point-move-selected pos])))]
    [:svg {:id preview-svg-id
           :viewBox (str "0 0 " (-> width (+ 20)) " " (-> height (+ 20)))
           :preserveAspectRatio "xMidYMid meet"
           :style {:width "100%"}
           :on-mouse-up route-path-point-mouse-up-fn
           :on-mouse-move (when route-path-point-move-fn
                            (fn [event]
                              (let [mx (.-clientX event)
                                    my (.-clientY event)
                                    [sx sy] (map-to-svg-space mx my)]
                                (route-path-point-move-fn (v/Vector. sx sy)))))}
     [:defs
      filter/shadow]
     [:g {:transform "translate(10,10)"}
      [:rect {:x 0
              :y 0
              :width width
              :height height
              :fill "#f6f6f6"
              :filter "url(#shadow)"}]
      (when-not (= edit-mode :none)
        [grid-lines width height 20 20])

      [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
       [render/ribbon (assoc render-context
                             :path ribbon-path) :argent :none :helmet-dark]
       [render-edit-overlay ribbon-path]

       (into [:<>]
             (map (fn [idx]
                    ^{:key [:point idx]}
                    [path-point (conj points-path idx)])
                  (range num-points)))

       (when (= edit-mode :add-or-remove)
         (into [:<>]
               (map (fn [[idx point]]
                      ^{:key [:add-or-remove idx]}
                      [add-point points-path idx point]))
               @(rf/subscribe [::edit-addable-points points-path])))]]]))

(defn- save-ribbon-clicked [event]
  (.preventDefault event)
  (.stopPropagation event)
  (let [payload @(rf/subscribe [:get form-db-path])
        user-data (user/data)]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (go
      (try
        (modal/start-loading)
        (let [response (<? (api.request/call :save-ribbon payload user-data))
              ribbon-id (:id response)]
          (rf/dispatch-sync [:set (conj form-db-path :id) ribbon-id])
          (rf/dispatch-sync [:set saved-data-db-path @(rf/subscribe [:get form-db-path])])
          (state/invalidate-cache-without-current form-db-path [ribbon-id nil])
          (state/invalidate-cache-without-current form-db-path [ribbon-id 0])
          (ribbon-select/invalidate-ribbons-cache)
          (rf/dispatch-sync [:set-form-message form-db-path
                             (string/str-tr :string.user.message/ribbon-saved (:version response))])
          (reife/push-state :view-ribbon-by-id {:id (id/for-url ribbon-id)}))
        (modal/stop-loading)
        (catch :default e
          (log/error "save-form error:" e)
          (rf/dispatch [:set-form-error form-db-path (:message (ex-data e))])
          (modal/stop-loading))))))

(defn- copy-to-new-clicked []
  (let [ribbon-data @(rf/subscribe [:get form-db-path])]
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (state/set-async-fetch-data
     form-db-path
     :new
     (dissoc ribbon-data
             :id
             :version
             :latest-version
             :username
             :user-id
             :created-at
             :first-version-created-at
             :name))
    (rf/dispatch-sync [:set-form-message form-db-path :string.user.message/created-unsaved-copy])
    (reife/push-state :create-ribbon)))

(defn- share-button-clicked []
  (let [url (entity.attribution/full-url-for-ribbon {:path form-db-path})]
    (rf/dispatch-sync [:clear-form-message form-db-path])
    (rf/dispatch-sync [:clear-form-errors form-db-path])
    (if (copy-to-clipboard url)
      (rf/dispatch [:set-form-message form-db-path :string.user.message/copied-url-for-sharing])
      (rf/dispatch [:set-form-error form-db-path :string.user.message/copy-to-clipboard-failed]))))

(defn- button-row []
  (let [error-message @(rf/subscribe [:get-form-error form-db-path])
        form-message @(rf/subscribe [:get-form-message form-db-path])
        ribbon-id @(rf/subscribe [:get (conj form-db-path :id)])
        ribbon-username @(rf/subscribe [:get (conj form-db-path :username)])
        public? (= @(rf/subscribe [:get (conj form-db-path :access)])
                   :public)
        user-data (user/data)
        logged-in? (:logged-in? user-data)
        unsaved-changes? (not= @(rf/subscribe [:get form-db-path])
                               @(rf/subscribe [:get saved-data-db-path]))
        saved? ribbon-id
        owned-by-me? (= (:username user-data) ribbon-username)
        can-copy? (and logged-in?
                       saved?
                       owned-by-me?)
        can-save? (and logged-in?
                       (or (not saved?)
                           owned-by-me?))
        can-share? (and public?
                        saved?
                        (not unsaved-changes?))]
    [:<>
     (when form-message
       [:div.success-message [tr form-message]])
     (when error-message
       [:div.error-message error-message])

     [:div.buttons {:style {:display "flex"}}
      [:div {:style {:flex "auto"}}]
      [:button.button {:style {:flex "initial"
                               :color "#777"}
                       :class (when-not can-share? "disabled")
                       :title (when-not can-share? (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))
                       :on-click (when can-share?
                                   share-button-clicked)}
       [:i.fas.fa-share-alt]]
      [hover-menu/hover-menu
       {:path [:arms-form-action-menu]}
       :string.button/actions
       [{:title :string.button/copy-to-new
         :icon "fas fa-clone"
         :handler copy-to-new-clicked
         :disabled? (not can-copy?)
         :tooltip (when-not can-copy?
                    (tr :string.user.message/need-to-be-logged-in-and-arms-must-be-saved))}
        {:title :string.button/share
         :icon "fas fa-share-alt"
         :handler share-button-clicked
         :disabled? (not can-share?)
         :tooltip (when-not can-share?
                    (tr :string.user.message/arms-need-to-be-public-and-saved-for-sharing))}]
       [:button.button {:style {:flex "initial"
                                :color "#777"
                                :margin-left "10px"}}
        [:i.fas.fa-ellipsis-h]]
       :require-click? true]
      [:button.button.primary
       {:type "submit"
        :class (when-not can-save? "disabled")
        :on-click (if can-save?
                    save-ribbon-clicked
                    #(js/alert "Need to be logged in and own the arms."))
        :style {:flex "initial"
                :margin-left "10px"}}
       [tr :string.button/save]]]]))

(defn- attribution []
  (let [attribution-data (attribution/for-ribbon {:path form-db-path})]
    [:div.attribution
     [:h3 [tr :string.attribution/title]]
     [:div {:style {:padding-left "1em"}}
      attribution-data]]))

(defn- edit-controls []
  (let [edit-mode @(rf/subscribe [::edit-mode])]
    [:div.no-select {:style {:position "absolute"
                             :left "20px"
                             :top "20px"}}
     [:button {:on-click #(rf/dispatch [::edit-toggle-show-points])
               :style (when-not (= edit-mode :none)
                        {:color "#ffffff"
                         :background-color "#ff8020"})}
      [tr :string.button/edit]]
     " "
     (when-not (= edit-mode :none)
       [tr :string.ribbon.editor/shift-info])]))

(defn- ribbon-form []
  (rf/dispatch [:set-title-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-ribbon])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  [:div {:style {:display "grid"
                 :grid-gap "10px"
                 :grid-template-columns "[start] auto [first] minmax(26em, 33%) [second] minmax(10em, 25%) [end]"
                 :grid-template-rows "[top] 100% [bottom]"
                 :grid-template-areas "'left middle right'"
                 :padding-right "10px"
                 :height "100%"}
         :on-click #(state/dispatch-on-event % [:ui-submenu-close-all])}
   [:div.no-scrollbar {:style {:grid-area "left"
                               :position "relative"}}
    [preview]
    [edit-controls]]

   [:div.no-scrollbar {:style {:grid-area "middle"
                               :padding-top "10px"}}
    [ui/selected-component]
    [button-row]
    [attribution]]
   [:div.no-scrollbar {:style {:grid-area "right"
                               :padding-top "5px"
                               :position "relative"}}
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path]]]])

(defn- ribbon-display [ribbon-id version]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path ribbon-id])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path ribbon-id]))
  (let [[status ribbon-data] (state/async-fetch-data
                              form-db-path
                              [ribbon-id version]
                              #(api/fetch-ribbon ribbon-id version saved-data-db-path))]
    (when (= status :done)
      (if ribbon-data
        [ribbon-form]
        [not-found/not-found]))))

(defn create [_match]
  (when @(rf/subscribe [:heraldicon.frontend.history.core/identifier-changed? form-db-path nil])
    (rf/dispatch-sync [:heraldicon.frontend.history.core/clear form-db-path nil]))
  (let [[status _ribbon-form-data] (state/async-fetch-data
                                    form-db-path
                                    :new
                                    #(go default/ribbon-entity))]
    (when (= status :done)
      [ribbon-form])))

(defn- on-select [{:keys [id]}]
  {:href (reife/href :view-ribbon-by-id {:id (id/for-url id)})
   :on-click (fn [_event]
               (rf/dispatch-sync [:clear-form-errors form-db-path])
               (rf/dispatch-sync [:clear-form-message form-db-path]))})

(defn view-list []
  (rf/dispatch [:set-title :string.menu/ribbon-library])
  [:div {:style {:padding "15px"}}
   [:div {:style {:text-align "justify"
                  :max-width "40em"}}
    [tr :string.text.ribbon-library/create-and-view-ribbons]]
   [:button.button.primary
    {:on-click #(do
                  (rf/dispatch-sync [:clear-form-errors form-db-path])
                  (rf/dispatch-sync [:clear-form-message form-db-path])
                  (reife/push-state :create-ribbon))}
    [tr :string.button/create]]
   [:div {:style {:padding-top "0.5em"}}
    [ribbon-select/list-ribbons on-select]]])

(defn view-by-id [{:keys [parameters]}]
  (let [id (-> parameters :path :id)
        version (-> parameters :path :version)
        ribbon-id (str "ribbon:" id)]
    (if (or (nil? version)
            (util/integer-string? version))
      [ribbon-display ribbon-id version]
      [not-found/not-found])))
