(ns heraldicon.frontend.core
  (:require
   [heraldicon.frontend.component.charge]
   [heraldicon.frontend.component.charge-group]
   [heraldicon.frontend.component.coat-of-arms]
   [heraldicon.frontend.component.cottise]
   [heraldicon.frontend.component.entity.arms.data]
   [heraldicon.frontend.component.entity.charge.data]
   [heraldicon.frontend.component.entity.collection.data]
   [heraldicon.frontend.component.entity.collection.element]
   [heraldicon.frontend.component.entity.core]
   [heraldicon.frontend.component.entity.ribbon.data]
   [heraldicon.frontend.component.field]
   [heraldicon.frontend.component.helm]
   [heraldicon.frontend.component.helms]
   [heraldicon.frontend.component.motto]
   [heraldicon.frontend.component.ordinary]
   [heraldicon.frontend.component.ornaments]
   [heraldicon.frontend.component.render-options]
   [heraldicon.frontend.component.semy]
   [heraldicon.frontend.component.shield-separator]
   [heraldicon.frontend.element.access]
   [heraldicon.frontend.element.arms-reference-select]
   [heraldicon.frontend.element.attributes]
   [heraldicon.frontend.element.attribution]
   [heraldicon.frontend.element.charge-group-preset-select]
   [heraldicon.frontend.element.charge-group-slot-number]
   [heraldicon.frontend.element.charge-group-type-select]
   [heraldicon.frontend.element.charge-type-select]
   [heraldicon.frontend.element.checkbox]
   [heraldicon.frontend.element.colours]
   [heraldicon.frontend.element.escutcheon-select]
   [heraldicon.frontend.element.field-layout]
   [heraldicon.frontend.element.field-type-select]
   [heraldicon.frontend.element.fimbriation]
   [heraldicon.frontend.element.flag-aspect-ratio-preset-select]
   [heraldicon.frontend.element.geometry]
   [heraldicon.frontend.element.hover-menu]
   [heraldicon.frontend.element.humetty]
   [heraldicon.frontend.element.line]
   [heraldicon.frontend.element.line-type-select]
   [heraldicon.frontend.element.metadata]
   [heraldicon.frontend.element.ordinary-type-select]
   [heraldicon.frontend.element.position]
   [heraldicon.frontend.element.radio-select]
   [heraldicon.frontend.element.range]
   [heraldicon.frontend.element.ribbon-reference-select]
   [heraldicon.frontend.element.select]
   [heraldicon.frontend.element.semy-layout]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.element.tags]
   [heraldicon.frontend.element.text-field]
   [heraldicon.frontend.element.theme-select]
   [heraldicon.frontend.element.tincture-modifiers]
   [heraldicon.frontend.element.tincture-select]
   [heraldicon.frontend.element.voided]
   [heraldicon.frontend.history.state]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.frontend.state :as state]
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
