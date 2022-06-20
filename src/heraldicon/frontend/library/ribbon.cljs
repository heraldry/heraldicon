(ns heraldicon.frontend.library.ribbon
  (:require
   [cljs.core.async :refer [go]]
   [heraldicon.frontend.attribution :as attribution]
   [heraldicon.frontend.entity.details :as details]
   [heraldicon.frontend.entity.details.buttons :as buttons]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.layout :as layout]
   [heraldicon.frontend.library.ribbon.shared :refer [form-id]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.message :as message]
   [heraldicon.frontend.title :as title]
   [heraldicon.frontend.ui.core :as ui]
   [heraldicon.heraldry.default :as default]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.interface :as interface]
   [heraldicon.math.curve.bezier :as bezier]
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.vector :as v]
   [heraldicon.render.core :as render]
   [heraldicon.svg.filter :as filter]
   [heraldicon.svg.path :as path]
   [re-frame.core :as rf]))

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

(defn- preview [form-db-path]
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

(defn- attribution [form-db-path]
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

(defn- ribbon-form [form-db-path]
  (rf/dispatch [::title/set-from-path-or-default
                (conj form-db-path :name)
                :string.text.title/create-ribbon])
  (rf/dispatch-sync [:ui-component-node-select-default form-db-path [form-db-path]])
  (layout/three-columns
   [:<>
    [preview form-db-path]
    [edit-controls]]
   [:<>
    [ui/selected-component]
    [message/display form-id]
    [buttons/buttons form-id]
    [attribution form-db-path]]
   [:<>
    [history/buttons form-db-path]
    [ui/component-tree [form-db-path]]]))

(defn create-view []
  [details/create-view form-id ribbon-form #(go default/ribbon-entity)])

(defn details-view [{{{:keys [id version]} :path} :parameters}]
  [details/by-id-view form-id (str "ribbon:" id) version ribbon-form])
