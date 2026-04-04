(ns heraldicon.frontend.tutorial
  (:require
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.tutorial.arms :as arms]
   [heraldicon.frontend.tutorial.charge :as charge]
   [heraldicon.frontend.tutorial.overview :as overview]
   [re-frame.core :as rf]
   [reagent.core :as r]))

(def ^:private db-path
  [:ui :tutorial])

(defn- goal-index-path [] (conj db-path :goal-index))
(defn- tour-id-path [] (conj db-path :tour-id))

(def ^:private tours
  {:overview {:goals overview/goals}
   :arms {:goals arms/goals}
   :charge {:goals charge/goals}})

(def ^:private menu-entries
  [{:label :string.tutorial/menu-overview
    :event [::overview/start]}
   {:label :string.tutorial/menu-arms-editor
    :event [::arms/start]}
   {:label :string.tutorial/menu-charge-editor
    :event [::charge/start]}])

(rf/reg-sub ::active-tour
  (fn [db _]
    (get-in db (tour-id-path))))

(rf/reg-sub ::goal-index
  (fn [db _]
    (get-in db (goal-index-path))))

(rf/reg-sub ::tour-goals
  (fn [_ _]
    (rf/subscribe [::active-tour]))
  (fn [tour-id _]
    (when tour-id
      (:goals (get tours tour-id)))))

(rf/reg-sub ::active-goal
  (fn [_ _]
    [(rf/subscribe [::tour-goals])
     (rf/subscribe [::goal-index])])
  (fn [[goals idx] _]
    (when (and goals idx (< idx (count goals)))
      (get goals idx))))

(rf/reg-sub ::goal-count
  (fn [_ _]
    (rf/subscribe [::tour-goals]))
  (fn [goals _]
    (count goals)))

(defn- condition-met? [db {:keys [path pred value]}]
  (let [current (get-in db path)]
    (cond
      pred (pred current)
      (some? value) (= current value)
      :else true)))

(rf/reg-sub ::active-hint
  (fn [db _]
    (let [tour-id (get-in db (tour-id-path))
          goal-idx (get-in db (goal-index-path))
          goals (:goals (get tours tour-id))
          goal (when (and goals goal-idx (< goal-idx (count goals)))
                 (get goals goal-idx))
          hints (:hints goal)]
      (reduce
       (fn [best hint]
         (let [{:keys [reached]} hint
               reached? (if reached
                          (condition-met? db reached)
                          true)]
           (if reached? hint best)))
       nil
       hints))))

(rf/reg-sub ::goal-complete?
  (fn [db _]
    (let [tour-id (get-in db (tour-id-path))
          goal-idx (get-in db (goal-index-path))
          goals (:goals (get tours tour-id))
          goal (when (and goals goal-idx (< goal-idx (count goals)))
                 (get goals goal-idx))
          complete-when (:complete-when goal)]
      (when complete-when
        (condition-met? db complete-when)))))

(defonce ^:private highlighted-element
  (atom nil))

(defn- clear-highlight! []
  (when-let [el @highlighted-element]
    (.remove (.-classList el) "tutorial-highlight")
    (reset! highlighted-element nil)))

(defn- set-highlight! [el]
  (clear-highlight!)
  (when el
    (.add (.-classList el) "tutorial-highlight")
    (reset! highlighted-element el)))

(rf/reg-event-db ::start
  (fn [db [_ tour-id]]
    (-> db
        (assoc-in (tour-id-path) tour-id)
        (assoc-in (goal-index-path) 0))))

(rf/reg-event-fx ::stop
  (fn [{:keys [db]} _]
    (clear-highlight!)
    {:db (assoc-in db db-path nil)}))

(rf/reg-event-db ::next-goal
  (fn [db _]
    (clear-highlight!)
    (let [idx (get-in db (goal-index-path))
          goals (:goals (get tours (get-in db (tour-id-path))))]
      (if (< (inc idx) (count goals))
        (assoc-in db (goal-index-path) (inc idx))
        (assoc-in db db-path nil)))))

(rf/reg-event-db ::previous-goal
  (fn [db _]
    (clear-highlight!)
    (let [idx (get-in db (goal-index-path))]
      (when (pos? idx)
        (assoc-in db (goal-index-path) (dec idx))))))

(def ^:private popover-gap 12)
(def ^:private arrow-size 8)
(def ^:private popover-width 340)
(def ^:private popover-margin 10)

(defn- clamp [v lo hi]
  (max lo (min v hi)))

(defn- compute-position [rect side]
  (let [scroll-x (.-scrollX js/window)
        scroll-y (.-scrollY js/window)
        vw (.-innerWidth js/window)
        cx (+ (.-left rect) (/ (.-width rect) 2) scroll-x)
        cy (+ (.-top rect) (/ (.-height rect) 2) scroll-y)
        half-w (/ popover-width 2)
        clamp-x #(clamp % (+ scroll-x popover-margin) (+ scroll-x vw (- popover-width) (- popover-margin)))]
    (case side
      "left" {:top cy
              :left (+ (.-left rect) scroll-x (- popover-gap) (- popover-width))
              :anchor :right
              :transform "translate(0, -50%)"}
      "right" {:top cy
               :left (+ (.-right rect) scroll-x popover-gap)
               :anchor :left
               :transform "translate(0, -50%)"}
      "top" {:top (+ (.-top rect) scroll-y (- popover-gap))
             :left (clamp-x (- cx half-w))
             :anchor :bottom
             :transform "translate(0, -100%)"}
      "bottom" {:top (+ (.-bottom rect) scroll-y popover-gap)
                :left (clamp-x (- cx half-w))
                :anchor :top}
      {:top (+ (.-bottom rect) scroll-y popover-gap)
       :left (clamp-x (- cx half-w))
       :anchor :top})))

(def ^:private border-arrow-size (+ arrow-size 1))

(defn- arrow-styles
  "Returns [border-style fill-style] for the arrow. The border arrow is 1px
   larger and sits behind the fill arrow to create the border effect."
  [anchor]
  (case anchor
    :top [{:top (- border-arrow-size)
           :left "50%"
           :transform "translate(-50%, 0)"
           :border-bottom-color "var(--tutorial-popover-border, #e8c96e)"
           :border-bottom-style "solid"
           :border-bottom-width (str border-arrow-size "px")
           :border-left (str border-arrow-size "px solid transparent")
           :border-right (str border-arrow-size "px solid transparent")}
          {:top (- arrow-size)
           :left "50%"
           :transform "translate(-50%, 0)"
           :border-bottom-color "var(--tutorial-popover-bg, #fff)"
           :border-bottom-style "solid"
           :border-bottom-width (str arrow-size "px")
           :border-left (str arrow-size "px solid transparent")
           :border-right (str arrow-size "px solid transparent")}]
    :bottom [{:bottom (- border-arrow-size)
              :left "50%"
              :transform "translate(-50%, 0)"
              :border-top-color "var(--tutorial-popover-border, #e8c96e)"
              :border-top-style "solid"
              :border-top-width (str border-arrow-size "px")
              :border-left (str border-arrow-size "px solid transparent")
              :border-right (str border-arrow-size "px solid transparent")}
             {:bottom (- arrow-size)
              :left "50%"
              :transform "translate(-50%, 0)"
              :border-top-color "var(--tutorial-popover-bg, #fff)"
              :border-top-style "solid"
              :border-top-width (str arrow-size "px")
              :border-left (str arrow-size "px solid transparent")
              :border-right (str arrow-size "px solid transparent")}]
    :left [{:top "50%"
            :left (- border-arrow-size)
            :transform "translate(0, -50%)"
            :border-right-color "var(--tutorial-popover-border, #e8c96e)"
            :border-right-style "solid"
            :border-right-width (str border-arrow-size "px")
            :border-top (str border-arrow-size "px solid transparent")
            :border-bottom (str border-arrow-size "px solid transparent")}
           {:top "50%"
            :left (- arrow-size)
            :transform "translate(0, -50%)"
            :border-right-color "var(--tutorial-popover-bg, #fff)"
            :border-right-style "solid"
            :border-right-width (str arrow-size "px")
            :border-top (str arrow-size "px solid transparent")
            :border-bottom (str arrow-size "px solid transparent")}]
    :right [{:top "50%"
             :right (- border-arrow-size)
             :transform "translate(0, -50%)"
             :border-left-color "var(--tutorial-popover-border, #e8c96e)"
             :border-left-style "solid"
             :border-left-width (str border-arrow-size "px")
             :border-top (str border-arrow-size "px solid transparent")
             :border-bottom (str border-arrow-size "px solid transparent")}
            {:top "50%"
             :right (- arrow-size)
             :transform "translate(0, -50%)"
             :border-left-color "var(--tutorial-popover-bg, #fff)"
             :border-left-style "solid"
             :border-left-width (str arrow-size "px")
             :border-top (str arrow-size "px solid transparent")
             :border-bottom (str arrow-size "px solid transparent")}]
    [nil nil]))

(defn- completion-watcher []
  (let [last-advanced (atom nil)
        prev-idx (atom nil)]
    (fn []
      (let [goal-idx @(rf/subscribe [::goal-index])
            goal @(rf/subscribe [::active-goal])
            complete? @(rf/subscribe [::goal-complete?])]
        (when (and @prev-idx goal-idx (< goal-idx @prev-idx))
          (reset! last-advanced goal-idx))
        (reset! prev-idx goal-idx)
        (when (and complete?
                   (not= @last-advanced goal-idx))
          (reset! last-advanced goal-idx)
          (js/setTimeout (fn []
                           (doseq [event (:on-complete goal)]
                             (rf/dispatch event))
                           (rf/dispatch [::next-goal]))
                         400)))
      nil)))

(defn- resolve-elements [{:keys [element popover-element]}]
  (let [highlight-el (when element (js/document.querySelector element))
        popover-el (or (when popover-element (js/document.querySelector popover-element))
                       highlight-el)]
    {:highlight-el highlight-el
     :popover-el popover-el}))

(defn- render-popover [goal goal-idx goal-count hint complete?]
  (let [{:keys [title description complete-when]} goal
        {:keys [side]} hint
        side (or side "bottom")
        {:keys [popover-el]} (resolve-elements hint)
        pos (when popover-el
              (let [rect (.getBoundingClientRect popover-el)]
                (when (pos? (.-width rect))
                  (.scrollIntoView popover-el #js {:behavior "smooth"
                                                   :block "nearest"})
                  (compute-position rect side))))
        last-goal? (= goal-idx (dec goal-count))]
    [:div.tutorial-popover
     {:style (if pos
               (cond-> {:position "absolute"
                        :top (:top pos)
                        :left (:left pos)}
                 (:transform pos) (assoc :transform (:transform pos)))
               {:position "fixed"
                :top "50%"
                :left "50%"
                :transform "translate(-50%, -50%)"})}
     (when pos
       (let [[border-style fill-style] (arrow-styles (:anchor pos))]
         [:<>
          (when border-style
            [:div.tutorial-arrow {:style border-style}])
          (when fill-style
            [:div.tutorial-arrow {:style fill-style}])]))
     [:div.tutorial-popover-header
      [:span.tutorial-progress
       (str (inc goal-idx) " / " goal-count)]
      [:button.tutorial-close
       {:on-click #(rf/dispatch [::stop])}
       "\u00d7"]]
     (when title
       [:div.tutorial-popover-title [tr title]])
     (when description
       [:div.tutorial-popover-description [tr description]])
     (when (and complete-when (not complete?))
       [:div.tutorial-popover-hint
        [tr :string.tutorial/hint]])
     [:div.tutorial-popover-footer
      (when (pos? goal-idx)
        [:button.tutorial-btn
         {:on-click #(rf/dispatch [::previous-goal])}
         [tr :string.tutorial/back]])
      (when (or (not complete-when) complete?)
        [:button.tutorial-btn.tutorial-btn-primary
         {:on-click #(rf/dispatch [::next-goal])}
         [tr (if last-goal? :string.tutorial/done :string.tutorial/next)]])]]))

(defn- apply-highlight! []
  (let [hint @(rf/subscribe [::active-hint])
        {:keys [highlight-el]} (resolve-elements hint)]
    (set-highlight! highlight-el)))

(defn- popover-content []
  (r/create-class
   {:component-did-mount apply-highlight!
    :component-did-update apply-highlight!
    :reagent-render
    (fn []
      (let [goal @(rf/subscribe [::active-goal])
            goal-idx @(rf/subscribe [::goal-index])
            goal-count @(rf/subscribe [::goal-count])
            hint @(rf/subscribe [::active-hint])
            complete? @(rf/subscribe [::goal-complete?])]
        (when goal
          [render-popover goal goal-idx goal-count hint complete?])))}))

(defn view []
  (let [tour-id @(rf/subscribe [::active-tour])]
    (when tour-id
      [:<>
       [completion-watcher]
       [popover-content]])))

(def ^:private menu-open?-path
  [:ui :menu :tutorial-menu :open?])

(rf/reg-sub ::menu-open?
  (fn [db _]
    (get-in db menu-open?-path)))

(rf/reg-event-db ::toggle-menu
  (fn [db _]
    (update-in db menu-open?-path not)))

(rf/reg-event-db ::close-menu
  (fn [db _]
    (assoc-in db menu-open?-path nil)))

(defn selector []
  [:li.nav-menu-item.nav-menu-has-children.nav-menu-allow-hover
   {:on-mouse-leave #(rf/dispatch [::close-menu])}
   [:<>
    [:a.nav-menu-link {:href "#"
                       :on-click (js-event/handled #(rf/dispatch [::toggle-menu]))}
     [:i.far.fa-question-circle] " " [tr :string.tutorial/menu]]
    [:ul.nav-menu.nav-menu-children
     {:style {:display (if @(rf/subscribe [::menu-open?])
                         "block"
                         "none")}}
     (doall
      (for [{:keys [label event]} menu-entries]
        ^{:key label}
        [:li.nav-menu-item
         [:a.nav-menu-link {:href "#"
                            :on-click (js-event/handled
                                       #(do (rf/dispatch [::close-menu])
                                            (rf/dispatch event)))}
          [tr label]]]))]]])
