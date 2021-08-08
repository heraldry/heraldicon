(ns heraldry.frontend.ribbon-library
  (:require [cljs.core.async :refer [go]]
            [clojure.string :as s]
            [com.wsscode.common.async-cljs :refer [<?]]
            [heraldry.coat-of-arms.catmullrom :as catmullrom]
            [heraldry.coat-of-arms.default :as default]
            [heraldry.coat-of-arms.filter :as filter]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.frontend.api.request :as api-request]
            [heraldry.frontend.attribution :as attribution]
            [heraldry.frontend.modal :as modal]
            [heraldry.frontend.ribbon :as ribbon]
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.core :as ui]
            [heraldry.frontend.user :as user]
            [heraldry.interface :as interface]
            [heraldry.util :as util :refer [id-for-url]]
            [re-frame.core :as rf]
            [reitit.frontend.easy :as reife]
            [taoensso.timbre :as log]))

(def form-db-path
  [:ribbon-form])

(def list-db-path
  [:ribbon-list])

(def example-coa-db-path
  [:example-coa])

(rf/reg-event-db :ribbon-edit-remove-point
  (fn [db [_ path]]
    (let [points-path (-> path drop-last vec)
          idx (last path)]
      (update-in db points-path
                 (fn [path]
                   (-> (concat (take idx path)
                               (drop (inc idx) path))
                       vec))))))

(rf/reg-event-db :ribbon-edit-toggle-show-points
  (fn [db _]
    (update-in db [:ui :ribbon-edit :show-points?] not)))

(rf/reg-event-db :ribbon-edit-add-point
  (fn [db [_ path]]
    (let [points-path (-> path drop-last vec)
          idx (last path)]
      (update-in db points-path
                 (fn [points]
                   (let [curve (vec (catmullrom/catmullrom points))
                         new-point (if (< idx (dec (count points)))
                                     (-> curve
                                         (get idx)
                                         (catmullrom/interpolate-point-cubic 0.5))
                                     (-> (v/+ (last points)
                                              {:x 50 :y 10})
                                         (update :x max -200)
                                         (update :x min 200)
                                         (update :y max -200)
                                         (update :y min 200)))]
                     (-> (concat (take (inc idx) points)
                                 [new-point]
                                 (drop (inc idx) points))
                         vec)))))))

(rf/reg-sub :ribbon-edit-point-deletable?
  (fn [[_ path] _]
    (let [points-path (-> path drop-last vec)]
      (rf/subscribe [:get-list-size points-path])))

  (fn [num-points [_ _path]]
    (> num-points 2)))

(rf/reg-sub :ribbon-edit-key-modifiers
  (fn [db _]
    (get-in db [:ui :ribbon-edit :key-modifiers])))

(rf/reg-event-db :ribbon-edit-set-key-modifiers
  (fn [db [_ key-modifiers]]
    (assoc-in db [:ui :ribbon-edit :key-modifiers] key-modifiers)))

(rf/reg-sub :selected-point
  (fn [db _]
    (get-in db [:ui :ribbon-edit :selected-point])))

(rf/reg-sub :ribbon-edit-selected-point
  (fn [_ _]
    (rf/subscribe [:get [:ui :ribbon-edit :selected-point :path]]))

  (fn [selected-point-path _]
    selected-point-path))

(rf/reg-sub :ribbon-edit-point-selected?
  (fn [[_ _path] _]
    (rf/subscribe [:get [:ui :ribbon-edit :selected-point :path]]))

  (fn [selected-point-path [_ path]]
    (= selected-point-path path)))

(rf/reg-sub :ribbon-edit-show-points?
  (fn [_ _]
    (rf/subscribe [:get [:ui :ribbon-edit :show-points?]]))

  (fn [show-points? _]
    show-points?))

(rf/reg-sub :ribbon-edit-mode
  (fn [_ _]
    [(rf/subscribe [:ribbon-edit-show-points?])
     (rf/subscribe [:ribbon-edit-key-modifiers])])

  (fn [[show-points? {:keys [shift? alt?]}] _]
    (if show-points?
      (cond
        shift? :add
        alt? :remove
        :else :edit)
      :none)))

(rf/reg-event-db :ribbon-edit-select-point
  (fn [db [_ path pos]]
    (if path
      (let [current-pos (get-in db path)
            dx (- (:x pos) (:x current-pos))
            dy (- (:y pos) (:y current-pos))]
        (assoc-in db [:ui :ribbon-edit :selected-point] {:path path
                                                         :dx dx
                                                         :dy dy}))
      (assoc-in db [:ui :ribbon-edit :selected-point] nil))))

(rf/reg-event-db :ribbon-edit-point-move-selected
  (fn [db [_ pos]]
    (let [{:keys [dx dy
                  path]} (get-in db [:ui :ribbon-edit :selected-point])]
      (if path
        (-> db
            (assoc-in (conj path :x) (-> pos :x (- dx)
                                         (max -200)
                                         (min 200)))
            (assoc-in (conj path :y) (-> pos :y (- dy)
                                         (max -200)
                                         (min 200))))
        db))))

(defn key-down-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)
        code (.-code event)]
    (when (= code "KeyE")
      (rf/dispatch [:ribbon-edit-toggle-show-points]))
    (rf/dispatch [:ribbon-edit-set-key-modifiers {:alt? alt?
                                                  :shift? shift?}])))

(defn key-up-handler [event]
  (let [shift? (.-shiftKey event)
        alt? (.-altKey event)]
    (rf/dispatch [:ribbon-edit-set-key-modifiers {:alt? alt?
                                                  :shift? shift?}])))

(def event-listener
  (do
    (js/window.removeEventListener "keydown" key-down-handler)
    (js/window.addEventListener "keydown" key-down-handler)
    (js/window.removeEventListener "keyup" key-up-handler)
    (js/window.addEventListener "keyup" key-up-handler)))


;; views


(def preview-svg-id
  "ribbon-preview")

(defn map-to-svg-space [x y]
  (let [svg (js/document.getElementById preview-svg-id)
        ctm (.getScreenCTM svg)]
    [(-> x
         (- (.-e ctm))
         (/ (.-a ctm)))
     (-> y
         (- (.-f ctm))
         (/ (.-d ctm)))]))

(defn path-point [path]
  (let [edit-mode @(rf/subscribe [:ribbon-edit-mode])]
    (if (= edit-mode :none)
      [:<>]
      (let [size 7
            width (* size 1.1)
            height (* size 0.2)
            {:keys [x y]} (interface/get-raw-data path {})
            deletable? @(rf/subscribe [:ribbon-edit-point-deletable? path])
            route-path-point-click-fn (case edit-mode
                                        :add #(rf/dispatch [:ribbon-edit-add-point %])
                                        :remove (when deletable?
                                                  #(rf/dispatch [:ribbon-edit-remove-point %]))
                                        nil)
            route-path-point-mouse-down-fn (when (= edit-mode :edit)
                                             (fn [pos]
                                               (rf/dispatch [:ribbon-edit-select-point path pos])))
            selected? @(rf/subscribe [:ribbon-edit-point-selected? path])]
        [:g
         {:transform (str "translate(" x "," y ")")
          :on-mouse-down (when route-path-point-mouse-down-fn
                           (fn [event]
                             (let [mx (.-clientX event)
                                   my (.-clientY event)
                                   [sx sy] (map-to-svg-space mx my)]
                               (route-path-point-mouse-down-fn {:x sx :y sy}))))
          :on-click (when route-path-point-click-fn
                      #(route-path-point-click-fn path))
          :style {:cursor "pointer"}}
         [:circle {:style {:fill (if selected?
                                   "#2c2"
                                   "#fff")
                           :stroke "#000"}
                   :r size}]
         (case edit-mode
           :add [:g
                 [:rect {:x (/ width -2)
                         :y (/ height -2)
                         :width width
                         :height height
                         :style {:fill "#000"}}]
                 [:rect {:x (/ height -2)
                         :y (/ width -2)
                         :width height
                         :height width
                         :style {:fill "#000"}}]]
           :remove (when deletable?
                     [:g
                      [:rect {:x (/ width -2)
                              :y (/ height -2)
                              :width width
                              :height height
                              :style {:fill "#000"}}]])
           nil)]))))

(defn curve-segments [full-curve
                      last-index end-t
                      index ts]
  (assert (-> ts count (<= 2)) "curve-segments only supports 2 tangent points per segment")
  (let [full-curve (vec full-curve)
        first-leg (when-not (= last-index index 0)
                    (cond-> (get full-curve last-index)
                      end-t (->
                             (catmullrom/split-bezier end-t)
                             :curve2)))]
    (if (empty? ts)
      [(-> (concat (when first-leg [first-leg])
                   (subvec full-curve (inc last-index) (inc index)))
           vec)]
      (let [[t1 t2] ts
            first-split (-> full-curve
                            (get index)
                            (catmullrom/split-bezier t1))]
        (cond-> [(-> (concat (when first-leg [first-leg])
                             (when (> index
                                      (inc last-index))
                               (subvec full-curve (inc last-index) index))
                             [(:curve1 first-split)])
                     vec)]
          t2 (conj [(-> (:curve2 first-split)
                        (catmullrom/split-bezier (/ (- t2 t1)
                                                    (- 1 t1)))
                        :curve1)]))))))

(defn split-curve [full-curve tangent-points]
  (if (empty? tangent-points)
    [full-curve]
    (->> (concat [[0 nil]]
                 tangent-points
                 [[(-> full-curve count dec) nil]])
         (partition 2 1)
         (mapcat (fn [[[last-index last-ts]
                       [index ts]]]
                   (curve-segments full-curve
                                   last-index (last last-ts)
                                   index ts)))
         vec)))

(defn render-ribbon [thickness]
  (let [points-path (conj form-db-path :ribbon :points)
        points (interface/get-raw-data points-path {})
        curve (catmullrom/catmullrom points)
        edge-vector (v/* (v/v 0 1) thickness)
        tangent-points (-> (keep-indexed
                            (fn [idx leg]
                              (let [ts (catmullrom/calculate-tangent-points leg ((juxt :x :y) edge-vector))]
                                (when (seq ts)
                                  [idx ts])))
                            curve))
        curves (split-curve curve tangent-points)]
    [:<>
     (doall
      (for [[idx partial-curve] (map-indexed vector curves)]
        (let [top-edge (catmullrom/curve->svg-path-relative partial-curve)
              full-path (str top-edge
                             (catmullrom/svg-line-to edge-vector)
                             (-> top-edge
                                 svg/reverse-path
                                 :path
                                 (s/replace "M0 0" ""))
                             (catmullrom/svg-line-to (v/* edge-vector -1)))]
          ^{:key idx}
          [:<>
           [:path {:d full-path
                   :style {:stroke-width 3
                           :stroke (if (odd? idx)
                                     "#ff8888"
                                     "#88ff88")
                           :stroke-linecap "round"
                           :fill (if (odd? idx)
                                   "#888888"
                                   "#dddddd")}}]])))]))

(defn preview []
  (let [[width height] [500 600]
        points-path (conj form-db-path :ribbon :points)
        num-points (interface/get-list-size points-path {})
        thickness (interface/get-sanitized-data (conj form-db-path :ribbon :thickness) {})
        thickness ((util/percent-of 300) thickness)
        edit-mode @(rf/subscribe [:ribbon-edit-mode])
        route-path-point-mouse-up-fn (when (= edit-mode :edit)
                                       #(rf/dispatch [:ribbon-edit-select-point nil]))
        route-path-point-move-fn (when (and (= edit-mode :edit)
                                            @(rf/subscribe [:ribbon-edit-selected-point]))
                                   (fn [pos]
                                     (rf/dispatch [:ribbon-edit-point-move-selected pos])))]
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
                                (route-path-point-move-fn {:x sx :y sy}))))}
     [:defs
      filter/shadow]
     [:g {:transform "translate(10,10)"}
      [:rect {:x 0
              :y 0
              :width width
              :height height
              :fill "#f6f6f6"
              :filter "url(#shadow)"}]
      [:g {:transform (str "translate(" (/ width 2) "," (/ height 2) ")")}
       [render-ribbon thickness]
       (doall
        (for [idx (range num-points)]
          ^{:key idx} [path-point (conj points-path idx)]))]]]))

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
  (let [[status ribbon-data] (state/async-fetch-data
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
                                       default/ribbon))]
    (when (= status :done)
      [ribbon-form])))

(defn view-list-ribbons []
  (rf/dispatch [:set-title "Ribbons"])
  (let [[status ribbons] (state/async-fetch-data
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

