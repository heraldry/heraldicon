(ns heraldry.frontend.ui.core
  (:require [heraldry.coat-of-arms.charge-group.options] ;; needed for defmethods
            [heraldry.coat-of-arms.charge.options] ;; needed for defmethods
            [heraldry.coat-of-arms.core] ;; needed for defmethods
            [heraldry.coat-of-arms.field.options] ;; needed for defmethods
            [heraldry.coat-of-arms.ordinary.options] ;; needed for defmethods
            [heraldry.coat-of-arms.semy.options] ;; needed for defmethods
            [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.arms-reference-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.attributes] ;; needed for defmethods
            [heraldry.frontend.ui.element.attribution] ;; needed for defmethods
            [heraldry.frontend.ui.element.charge-group-preset-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.charge-group-slot-number] ;; needed for defmethods
            [heraldry.frontend.ui.element.charge-group-type-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.charge-type-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.checkbox] ;; needed for defmethods
            [heraldry.frontend.ui.element.colours] ;; needed for defmethods
            [heraldry.frontend.ui.element.escutcheon-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.field-layout] ;; needed for defmethods
            [heraldry.frontend.ui.element.field-type-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.fimbriation] ;; needed for defmethods
            [heraldry.frontend.ui.element.geometry] ;; needed for defmethods
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.frontend.ui.element.line] ;; needed for defmethods
            [heraldry.frontend.ui.element.line-type-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.ordinary-type-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.position] ;; needed for defmethods
            [heraldry.frontend.ui.element.radio-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.range] ;; needed for defmethods
            [heraldry.frontend.ui.element.select] ;; needed for defmethods
            [heraldry.frontend.ui.element.semy-layout] ;; needed for defmethods
            [heraldry.frontend.ui.element.submenu] ;; needed for defmethods
            [heraldry.frontend.ui.element.tags] ;; needed for defmethods
            [heraldry.frontend.ui.element.text-field] ;; needed for defmethods
            [heraldry.frontend.ui.element.theme-select] ;; needed for defmethods
            [heraldry.frontend.ui.element.tincture-modifiers] ;; needed for defmethods
            [heraldry.frontend.ui.element.tincture-select] ;; needed for defmethods
            [heraldry.frontend.ui.form.arms-general] ;; needed for defmethods
            [heraldry.frontend.ui.form.charge] ;; needed for defmethods
            [heraldry.frontend.ui.form.charge-general] ;; needed for defmethods
            [heraldry.frontend.ui.form.charge-group] ;; needed for defmethods
            [heraldry.frontend.ui.form.coat-of-arms] ;; needed for defmethods
            [heraldry.frontend.ui.form.collection] ;; needed for defmethods
            [heraldry.frontend.ui.form.collection-element] ;; needed for defmethods
            [heraldry.frontend.ui.form.collection-general] ;; needed for defmethods
            [heraldry.frontend.ui.form.cottise] ;; needed for defmethods
            [heraldry.frontend.ui.form.field] ;; needed for defmethods
            [heraldry.frontend.ui.form.ordinary] ;; needed for defmethods
            [heraldry.frontend.ui.form.render-options] ;; needed for defmethods
            [heraldry.frontend.ui.form.semy] ;; needed for defmethods
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.license] ;; needed for defmethods
            [heraldry.render-options] ;; needed for defmethods
            [re-frame.core :as rf]))

(def node-flag-db-path [:ui :component-tree :nodes])
(def ui-component-node-selected-path [:ui :component-tree :selected-node])

;; subs

(rf/reg-sub :ui-component-node-open?
  (fn [[_ path] _]
    (rf/subscribe [:get (conj node-flag-db-path path)]))

  (fn [flag [_ _path]]
    flag))

(rf/reg-sub :ui-component-node-selected-path
  (fn [_ _]
    (rf/subscribe [:get ui-component-node-selected-path]))

  (fn [selected-node-path [_ _path]]
    selected-node-path))

(rf/reg-sub :component-data
  (fn [db [_ path]]
    (let [data (get-in db path)]
      (cond-> data
        (and (map? data)
             (-> data :type not)) (assoc :type (keyword "heraldry.component" (last path)))))))

(rf/reg-sub :component-node
  (fn [[_ path] _]
    [(rf/subscribe [:component-data path])
     (rf/subscribe [:get-relevant-options path])
     (rf/subscribe [:ui-component-node-open? path])
     (rf/subscribe [:ui-component-node-selected-path])])

  (fn [[component-data component-options open? selected-component-path] [_ path]]
    (merge {:open? open?
            :selected? (= path selected-component-path)
            :selectable? true}
           (interface/component-node-data path component-data component-options))))

(rf/reg-sub :component-form
  (fn [[_ path] _]
    [(rf/subscribe [:component-node path])
     (rf/subscribe [:component-data path])])

  (fn [[{:keys [title]} component-data] [_ path]]
    (merge
     {:title title
      :path path}
     (interface/component-form-data path component-data nil))))

;; events

(rf/reg-event-db :ui-component-node-open
  (fn [db [_ path]]
    (let [path (vec path)]
      (->> (range (count path))
           (map (fn [idx]
                  [(subvec path 0 (inc idx)) true]))
           (into {})
           (update-in db node-flag-db-path merge)))))

(rf/reg-event-db :ui-component-node-close
  (fn [db [_ path]]
    (update-in
     db node-flag-db-path
     (fn [flags]
       (->> flags
            (filter (fn [[other-path v]]
                      (when (not= (take (count path) other-path)
                                  path)
                        [other-path v])))
            (into {}))))))

(rf/reg-event-fx :ui-component-node-toggle
  (fn [{:keys [db]} [_ path]]
    {:dispatch (if (get-in db (conj node-flag-db-path path))
                 [:ui-component-node-close path]
                 [:ui-component-node-open path])}))

(rf/reg-event-fx :ui-component-node-select
  (fn [{:keys [db]} [_ path {:keys [open?]}]]
    {:db (assoc-in db ui-component-node-selected-path path)
     :dispatch [:ui-component-node-open (cond-> path
                                          (not open?) drop-last)]}))

(rf/reg-event-fx :ui-component-node-select-default
  (fn [{:keys [db]} [_ path]]
    (let [old-path (get-in db ui-component-node-selected-path)
          new-path (if (or (not old-path)
                           (not= (subvec old-path 0 (count path))
                                 path))
                     path
                     old-path)]
      (cond-> {}
        (not= new-path
              old-path) (assoc :dispatch [:ui-component-node-select new-path])))))

(rf/reg-event-fx :add-component
  (fn [{:keys [db]} [_ path value]]
    (let [components-path (conj path :components)
          index (count (get-in db components-path))]
      {:db (update-in db components-path #(-> %
                                              (conj value)
                                              vec))
       :dispatch [:ui-component-node-select (conj components-path index) {:open? true}]})))

(rf/reg-event-fx :add-element
  (fn [{:keys [db]} [_ path value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)
          element-path (conj path (-> elements count dec))]
      {:db (assoc-in db path elements)
       :dispatch [:ui-component-node-select element-path {:open? true}]})))

(rf/reg-event-db :remove-element
  (fn [db [_ path]]
    (let [elements-path (-> path drop-last vec)
          index (last path)
          elements (vec (get-in db elements-path))
          num-elements (count elements)]
      (if (>= index num-elements)
        db
        (-> db
            (update-in elements-path (fn [elements]
                                       (vec (concat (subvec elements 0 index)
                                                    (subvec elements (inc index))))))
            (update-in ui-component-node-selected-path
                       (fn [selected-node-path]
                         (let [selected-base-path (drop-last selected-node-path)
                               selected-index (last selected-node-path)]
                           (cond
                             (and (= selected-base-path elements-path)
                                  (= selected-index index)) nil
                             (and (= selected-base-path elements-path)
                                  (> selected-index index)) (conj elements-path (dec selected-index))
                             :else selected-node-path)))))))))

(rf/reg-event-db :move-element-up
  (fn [db [_ path]]
    (let [elements-path (-> path drop-last vec)
          index (last path)
          elements (vec (get-in db elements-path))
          num-elements (count elements)]
      (if (>= index num-elements)
        db
        (-> db
            (assoc-in elements-path (-> elements
                                        (subvec 0 index)
                                        (conj (get elements (inc index)))
                                        (conj (get elements index))
                                        (concat (subvec elements (+ index 2)))
                                        vec))
            (update-in ui-component-node-selected-path
                       (fn [selected-node-path]
                         (let [selected-base-path (drop-last selected-node-path)
                               selected-index (last selected-node-path)]
                           (cond
                             (and (= selected-base-path elements-path)
                                  (= selected-index (inc index))) (conj elements-path index)
                             (and (= selected-base-path elements-path)
                                  (= selected-index index)) (conj elements-path (inc index))
                             :else selected-node-path)))))))))

(rf/reg-event-db :move-element-down
  (fn [db [_ path]]
    (let [elements-path (-> path drop-last vec)
          index (last path)]
      (if (zero? index)
        db
        (-> db
            (update-in elements-path (fn [elements]
                                       (-> elements
                                           vec
                                           (subvec 0 (dec index))
                                           (conj (get elements index))
                                           (conj (get elements (dec index)))
                                           (concat (subvec elements (inc index)))
                                           vec)))
            (update-in ui-component-node-selected-path
                       (fn [selected-node-path]
                         (let [selected-base-path (drop-last selected-node-path)
                               selected-index (last selected-node-path)]
                           (cond
                             (and (= selected-base-path elements-path)
                                  (= selected-index (dec index))) (conj elements-path index)
                             (and (= selected-base-path elements-path)
                                  (= selected-index index)) (conj elements-path (dec index))
                             :else selected-node-path)))))))))



;; functions


(defn component-node [path & {:keys [title parent-buttons]}]
  (let [node-data @(rf/subscribe [:component-node path])
        {node-title :title
         open? :open?
         selected? :selected?
         selectable? :selectable?
         nodes :nodes
         buttons :buttons} node-data
        openable? (-> nodes count pos?)
        title (or node-title title)
        buttons (concat buttons parent-buttons)]
    [:<>
     [:div.node-name.clickable.no-select
      {:class (when selected?
                "selected")
       :on-click #(do
                    (when (or (not open?)
                              (not selectable?)
                              selected?)
                      (rf/dispatch [:ui-component-node-toggle path]))
                    (when selectable?
                      (rf/dispatch [:ui-component-node-select path]))
                    (.stopPropagation %))}
      (if openable?
        [:span.node-icon.clickable
         {:on-click #(state/dispatch-on-event % [:ui-component-node-toggle path])}
         [:i.fa.ui-icon {:class (if open?
                                  "fa-angle-down"
                                  "fa-angle-right")}]]
        [:span.node-icon
         [:i.fa.ui-icon.fa-angle-down {:style {:opacity 0}}]])
      title
      (when (seq buttons)
        (doall
         (for [[idx {:keys [icon menu handler disabled? tooltip title]}] (map-indexed vector buttons)]
           (if menu
             ^{:key idx}
             [hover-menu/hover-menu
              (conj path idx)
              title
              menu
              [:i.ui-icon {:class icon
                           :style {:margin-left "0.5em"
                                   :font-size "0.8em"
                                   :color (if disabled?
                                            "#ccc"
                                            "#777")}}]
              :disabled? disabled?]
             ^{:key icon} [:span.node-icon
                           {:class (when disabled? "disabled")
                            :on-click (when-not disabled? handler)
                            :title tooltip}
                           [:i.ui-icon {:class icon
                                        :style {:margin-left "0.5em"
                                                :font-size "0.8em"
                                                :color (if disabled?
                                                         "#ccc"
                                                         "#777")}}]]))))]
     (when open?
       [:ul
        (for [{node-path :path
               title :title
               buttons :buttons} nodes]
          ^{:key node-path} [:li [component-node node-path :title title :parent-buttons buttons]])])]))

(defn component-tree [paths]
  [:div.ui-tree
   [:ul
    (for [[idx node-path] (map-indexed vector paths)]
      ^{:key idx} [:li [component-node node-path]])]])

(defn component-form [path]
  (let [{:keys [title path form form-args]} (when path
                                              @(rf/subscribe [:component-form path]))]
    [:div.ui-component
     [:div.ui-component-header
      [:h1
       [:i.fa.fa-sliders-h.ui-icon {:style {:margin-right "0.5em"}}]
       title]]
     [:div.content
      (when form
        [form path form-args])]]))

(defn selected-component []
  (let [selected-component-path @(rf/subscribe [:ui-component-node-selected-path])]
    [component-form selected-component-path]))
