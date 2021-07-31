(ns heraldry.frontend.ui.core
  (:require [heraldry.frontend.state :as state]
            [heraldry.frontend.ui.element.arms-reference-select] ;; needed for side effects
            [heraldry.frontend.ui.element.attributes] ;; needed for side effects
            [heraldry.frontend.ui.element.attribution] ;; needed for side effects
            [heraldry.frontend.ui.element.charge-group-preset-select] ;; needed for side effects
            [heraldry.frontend.ui.element.charge-group-slot-number] ;; needed for side effects
            [heraldry.frontend.ui.element.charge-group-type-select] ;; needed for side effects
            [heraldry.frontend.ui.element.charge-type-select] ;; needed for side effects
            [heraldry.frontend.ui.element.checkbox] ;; needed for side effects
            [heraldry.frontend.ui.element.colours] ;; needed for side effects
            [heraldry.frontend.ui.element.escutcheon-select] ;; needed for side effects
            [heraldry.frontend.ui.element.field-layout] ;; needed for side effects
            [heraldry.frontend.ui.element.field-type-select] ;; needed for side effects
            [heraldry.frontend.ui.element.fimbriation] ;; needed for side effects
            [heraldry.frontend.ui.element.geometry] ;; needed for side effects
            [heraldry.frontend.ui.element.hover-menu :as hover-menu]
            [heraldry.frontend.ui.element.line] ;; needed for side effects
            [heraldry.frontend.ui.element.line-type-select] ;; needed for side effects
            [heraldry.frontend.ui.element.ordinary-type-select] ;; needed for side effects
            [heraldry.frontend.ui.element.position] ;; needed for side effects
            [heraldry.frontend.ui.element.radio-select] ;; needed for side effects
            [heraldry.frontend.ui.element.range] ;; needed for side effects
            [heraldry.frontend.ui.element.select] ;; needed for side effects
            [heraldry.frontend.ui.element.semy-layout] ;; needed for side effects
            [heraldry.frontend.ui.element.submenu :as submenu]
            [heraldry.frontend.ui.element.tags] ;; needed for side effects
            [heraldry.frontend.ui.element.text-field] ;; needed for side effects
            [heraldry.frontend.ui.element.theme-select] ;; needed for side effects
            [heraldry.frontend.ui.element.tincture-modifiers] ;; needed for side effects
            [heraldry.frontend.ui.element.tincture-select] ;; needed for side effects
            [heraldry.frontend.ui.form.arms-general] ;; needed for side effects
            [heraldry.frontend.ui.form.charge] ;; needed for side effects
            [heraldry.frontend.ui.form.charge-general] ;; needed for side effects
            [heraldry.frontend.ui.form.charge-group] ;; needed for side effects
            [heraldry.frontend.ui.form.coat-of-arms] ;; needed for side effects
            [heraldry.frontend.ui.form.collection] ;; needed for side effects
            [heraldry.frontend.ui.form.collection-element] ;; needed for side effects
            [heraldry.frontend.ui.form.collection-general] ;; needed for side effects
            [heraldry.frontend.ui.form.cottise] ;; needed for side effects
            [heraldry.frontend.ui.form.field] ;; needed for side effects
            [heraldry.frontend.ui.form.helm] ;; needed for side effects
            [heraldry.frontend.ui.form.helms] ;; needed for side effects
            [heraldry.frontend.ui.form.ordinary] ;; needed for side effects
            [heraldry.frontend.ui.form.render-options] ;; needed for side effects
            [heraldry.frontend.ui.form.semy] ;; needed for side effects
            [heraldry.frontend.ui.interface :as ui-interface]
            [heraldry.frontend.validation :as validation] ;; needed for side effects
            [heraldry.interface :as interface]
            [heraldry.shared] ;; needed for side effects
            [heraldry.util :as util]
            [re-frame.core :as rf]))

;; subs


(rf/reg-sub :component-node
  (fn [[_ path] _]
    [(rf/subscribe [:ui-component-node-open? path])
     (rf/subscribe [:ui-component-node-selected-path])])

  (fn [[open? selected-component-path] [_ path]]
    (merge {:open? open?
            :selected? (= path selected-component-path)
            :selectable? true}
           (ui-interface/component-node-data path))))

(rf/reg-sub :component-form
  (fn [[_ path] _]
    (rf/subscribe [:component-node path]))

  (fn [{:keys [title]} [_ path]]
    (merge
     {:title title
      :path path}
     (ui-interface/component-form-data path))))

;; events

(rf/reg-event-db :add-element
  (fn [db [_ path value]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)
          element-path (conj path (-> elements count dec))
          added-type (interface/effective-component-type element-path (:type value))]
      (-> db
          (assoc-in path elements)
          (state/ui-component-node-select element-path :open? true)
          submenu/ui-submenu-close-all
          (cond->
           (#{:heraldry.component/ordinary
              :heraldry.component/charge} added-type) (submenu/ui-submenu-open (conj element-path :type))
           (#{:heraldry.component/charge-group} added-type) (submenu/ui-submenu-open element-path)
           (#{:heraldry.component/collection-element} added-type) (submenu/ui-submenu-open (conj element-path :reference)))))))

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
            (state/element-order-changed elements-path index nil))))))

(rf/reg-event-db :move-element
  (fn [db [_ path new-index]]
    (let [elements-path (-> path drop-last vec)
          elements (vec (get-in db elements-path))
          index (last path)
          new-index (-> new-index
                        (max 0)
                        (min (dec (count elements))))]
      (if (or (= index new-index)
              (neg? new-index))
        db
        (-> db
            (update-in elements-path util/vec-move index new-index)
            (state/element-order-changed elements-path index new-index))))))


;; functions


(defn component-node [path & {:keys [title parent-buttons]}]
  (let [node-data @(rf/subscribe [:component-node path])
        node-title (:title node-data)
        {:keys [open?
                selected?
                selectable?
                nodes
                buttons
                validation]} node-data
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
                    (.stopPropagation %))
       :style {:color (when-not selectable?
                        "#000")}}
      (if openable?
        [:span.node-icon.clickable
         {:on-click #(state/dispatch-on-event % [:ui-component-node-toggle path])}
         [:i.fa.ui-icon {:class (if open?
                                  "fa-angle-down"
                                  "fa-angle-right")}]]
        [:span.node-icon
         [:i.fa.ui-icon.fa-angle-down {:style {:opacity 0}}]])
      title

      (validation/render validation)

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
                                            "#777")
                                   :cursor (when disabled? "not-allowed")}}]
              :disabled? disabled?
              :require-click? true]
             ^{:key icon} [:span.node-icon
                           {:class (when disabled? "disabled")
                            :on-click (when-not disabled? handler)
                            :title tooltip}
                           [:i.ui-icon {:class icon
                                        :style {:margin-left "0.5em"
                                                :font-size "0.8em"
                                                :color (if disabled?
                                                         "#ccc"
                                                         "#777")
                                                :cursor (when disabled? "not-allowed")}}]]))))]
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
