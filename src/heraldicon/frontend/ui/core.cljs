(ns heraldicon.frontend.ui.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.history.state]
   [heraldicon.frontend.language :refer [tr]]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
   [heraldicon.frontend.ui.element.access]
   [heraldicon.frontend.ui.element.arms-reference-select]
   [heraldicon.frontend.ui.element.attributes]
   [heraldicon.frontend.ui.element.attribution]
   [heraldicon.frontend.ui.element.charge-group-preset-select]
   [heraldicon.frontend.ui.element.charge-group-slot-number]
   [heraldicon.frontend.ui.element.charge-group-type-select]
   [heraldicon.frontend.ui.element.charge-type-select]
   [heraldicon.frontend.ui.element.checkbox]
   [heraldicon.frontend.ui.element.colours]
   [heraldicon.frontend.ui.element.escutcheon-select]
   [heraldicon.frontend.ui.element.field-layout]
   [heraldicon.frontend.ui.element.field-type-select]
   [heraldicon.frontend.ui.element.fimbriation]
   [heraldicon.frontend.ui.element.flag-aspect-ratio-preset-select]
   [heraldicon.frontend.ui.element.geometry]
   [heraldicon.frontend.ui.element.hover-menu :as hover-menu]
   [heraldicon.frontend.ui.element.humetty]
   [heraldicon.frontend.ui.element.line]
   [heraldicon.frontend.ui.element.line-type-select]
   [heraldicon.frontend.ui.element.metadata]
   [heraldicon.frontend.ui.element.ordinary-type-select]
   [heraldicon.frontend.ui.element.position]
   [heraldicon.frontend.ui.element.radio-select]
   [heraldicon.frontend.ui.element.range]
   [heraldicon.frontend.ui.element.ribbon-reference-select]
   [heraldicon.frontend.ui.element.select]
   [heraldicon.frontend.ui.element.semy-layout]
   [heraldicon.frontend.ui.element.submenu :as submenu]
   [heraldicon.frontend.ui.element.tags]
   [heraldicon.frontend.ui.element.text-field]
   [heraldicon.frontend.ui.element.theme-select]
   [heraldicon.frontend.ui.element.tincture-modifiers]
   [heraldicon.frontend.ui.element.tincture-select]
   [heraldicon.frontend.ui.element.voided]
   [heraldicon.frontend.ui.form.charge]
   [heraldicon.frontend.ui.form.charge-group]
   [heraldicon.frontend.ui.form.coat-of-arms]
   [heraldicon.frontend.ui.form.cottise]
   [heraldicon.frontend.ui.form.entity.arms.data]
   [heraldicon.frontend.ui.form.entity.charge.data]
   [heraldicon.frontend.ui.form.entity.collection.data]
   [heraldicon.frontend.ui.form.entity.collection.element]
   [heraldicon.frontend.ui.form.entity.core]
   [heraldicon.frontend.ui.form.entity.ribbon.data]
   [heraldicon.frontend.ui.form.field]
   [heraldicon.frontend.ui.form.helm]
   [heraldicon.frontend.ui.form.helms]
   [heraldicon.frontend.ui.form.motto]
   [heraldicon.frontend.ui.form.ordinary]
   [heraldicon.frontend.ui.form.ornaments]
   [heraldicon.frontend.ui.form.render-options]
   [heraldicon.frontend.ui.form.semy]
   [heraldicon.frontend.ui.form.shield-separator]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.shared]
   [heraldicon.util.vec :as vec]
   [re-frame.core :as rf]))

(macros/reg-event-db :add-element
  (fn [db [_ {:keys [path]} value {:keys [post-fn selected-element-path-fn]}]]
    (let [elements (-> (get-in db path)
                       (conj value)
                       vec)
          elements (if post-fn
                     (post-fn elements)
                     elements)
          new-element-path (conj path (-> elements count dec))
          new-element-path (if selected-element-path-fn
                             (selected-element-path-fn new-element-path (last elements) elements)
                             new-element-path)
          added-type (component/effective-type (:type value))]
      (-> db
          (assoc-in path elements)
          (state/ui-component-node-select
           (if (isa? added-type :heraldry/helm)
             (conj new-element-path :components 1)
             new-element-path)
           :open? true)
          submenu/ui-submenu-close-all
          (cond->
            (isa? added-type :heraldry/helm) (submenu/ui-submenu-open (conj new-element-path :components 1 :type))
            (isa? added-type :heraldry/ordinary) (submenu/ui-submenu-open (conj new-element-path :type))
            (isa? added-type :heraldry/charge) (submenu/ui-submenu-open (conj new-element-path :type))
            (isa? added-type :heraldry/charge-group) (submenu/ui-submenu-open new-element-path)
            (isa? added-type :heraldry/motto) (submenu/ui-submenu-open (conj new-element-path :ribbon-variant))
            (isa? added-type :heraldicon.entity.collection/element) (submenu/ui-submenu-open (conj new-element-path :reference)))))))

(macros/reg-event-db :remove-element
  (fn [db [_ {:keys [path]} {:keys [post-fn]}]]
    (let [elements-path (-> path drop-last vec)
          index (last path)
          elements (vec (get-in db elements-path))
          num-elements (count elements)]
      (if (>= index num-elements)
        db
        (-> db
            (update-in elements-path (fn [elements]
                                       (cond-> (vec (concat (subvec elements 0 index)
                                                            (subvec elements (inc index))))
                                         post-fn post-fn)))
            (state/element-order-changed elements-path index nil))))))

(macros/reg-event-db :move-element
  (fn [db [_ {:keys [path]} new-index]]
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
            (update-in elements-path vec/move-element index new-index)
            (state/element-order-changed elements-path index new-index))))))

(rf/reg-sub :element-removable?
  (fn [[_ {:keys [path]}] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [element-type _context]
    (not (shield-separator/shield-separator? {:type element-type}))))

(defn- raw-component-node [{:keys [path] :as context}]
  (merge {:open? @(rf/subscribe [:ui-component-node-open? path])
          :selected? (= path @(rf/subscribe [:ui-component-node-selected-path]))
          :selectable? true}
         (ui.interface/component-node-data context)))

(defn- component-node [{:keys [path] :as context} & {:keys [title parent-buttons]}]
  (let [node-data (raw-component-node context)
        node-title (:title node-data)
        {:keys [open?
                selected?
                selectable?
                nodes
                buttons
                annotation
                validation
                icon]} node-data
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
         {:on-click #(state/dispatch-on-event % [:ui-component-node-toggle path])
          :style {:width "0.9em"}}
         [:i.fa.ui-icon {:class (if open?
                                  "fa-angle-down"
                                  "fa-angle-right")}]]
        [:span.node-icon
         [:i.fa.ui-icon.fa-angle-down {:style {:opacity 0}}]])

      (when icon
        (let [effective-icon (if selected?
                               (:selected icon)
                               (:default icon))
              icon-style {:width "14px"
                          :height "16px"
                          :margin-right "5px"
                          :vertical-align "top"
                          :transform "translate(0,3px)"}]
          (if (vector? effective-icon)
            (update-in effective-icon [1 :style] merge icon-style)
            [:img {:src effective-icon
                   :style icon-style}])))

      [tr title]

      annotation

      (validation/render validation)

      (into [:<>]
            (comp (filter :menu)
                  (map-indexed (fn [idx {:keys [icon menu disabled? title]}]
                                 [hover-menu/hover-menu
                                  (c/++ context idx)
                                  title
                                  menu
                                  [:i.ui-icon {:class icon
                                               :title (tr title)
                                               :style {:margin-left "0.5em"
                                                       :font-size "0.8em"
                                                       :color (if disabled?
                                                                "#ccc"
                                                                "#777")
                                                       :cursor (when disabled? "not-allowed")}}]
                                  :disabled? disabled?
                                  :require-click? true])))
            buttons)

      (into [:span {:style {:margin-left "0.5em"}}]
            (comp (remove :menu)
                  (map-indexed (fn [idx {:keys [icon handler disabled? title remove?]}]
                                 [:span.node-icon
                                  {:class (when disabled? "disabled")
                                   :title (tr title)
                                   :style {:margin-left (when (and (pos? idx)
                                                                   remove?) "0.5em")}}
                                  [:i.ui-icon {:class icon
                                               :on-click (when-not disabled? handler)
                                               :style {:font-size "0.8em"
                                                       :color (if disabled?
                                                                "#ccc"
                                                                "#777")
                                                       :cursor (when disabled? "not-allowed")}}]])))
            buttons)]

     (when open?
       (into [:ul]
             (map (fn [{:keys [context title buttons]}]
                    ^{:key context}
                    [:li [component-node context :title title :parent-buttons buttons]]))
             nodes))]))

(def ^:private node-render-options
  {:render-options {:type :heraldry/render-options}
   :render-options-path [:context :render-options]})

(defn component-tree [paths]
  [:div.ui-tree
   (into [:ul]
         (map-indexed (fn [idx node-path]
                        ^{:key idx}
                        [:li
                         (if (= node-path :spacer)
                           [:div {:style {:height "1em"}}]
                           [component-node (merge node-render-options
                                                  {:path node-path})])]))
         paths)])

(defn- raw-component-form [context]
  (let [node-data (raw-component-node context)]
    (merge
     {:title (:title node-data)
      :context context}
     (ui.interface/component-form-data context))))

(defn- component-form [context]
  (let [{:keys [title context form]} (when context
                                       (raw-component-form context))]
    [:div.ui-component
     [:div.ui-component-header
      [:h1
       [:i.fa.fa-sliders-h.ui-icon {:style {:margin-right "0.5em"}}]
       [tr title]]]
     [:div.content
      (when form
        [form context])]]))

(defn selected-component []
  (let [selected-component-path @(rf/subscribe [:ui-component-node-selected-path])]
    [component-form (when selected-component-path
                      (merge node-render-options
                             {:path selected-component-path}))]))
