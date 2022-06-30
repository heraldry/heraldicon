(ns heraldicon.frontend.component.element
  (:require
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.util.vec :as vec]
   [re-frame.core :as rf]))

(macros/reg-event-db ::add
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
          (tree/select-node
           (if (isa? added-type :heraldry/helm)
             (conj new-element-path :components 1)
             new-element-path)
           :open? true)
          submenu/close-all
          (cond->
            (isa? added-type :heraldry/helm) (submenu/open (conj new-element-path :components 1 :type))
            (isa? added-type :heraldry/ordinary) (submenu/open (conj new-element-path :type))
            (isa? added-type :heraldry/charge) (submenu/open (conj new-element-path :type))
            (isa? added-type :heraldry/charge-group) (submenu/open new-element-path)
            (isa? added-type :heraldry/motto) (submenu/open (conj new-element-path :ribbon-variant))
            (isa? added-type :heraldicon.entity.collection/element) (submenu/open (conj new-element-path :reference)))))))

(macros/reg-event-db ::remove
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
            (tree/element-order-changed elements-path index nil))))))

(macros/reg-event-db ::move
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
            (tree/element-order-changed elements-path index new-index))))))

(rf/reg-sub ::removable?
  (fn [[_ {:keys [path]}] _]
    (rf/subscribe [:get (conj path :type)]))

  (fn [element-type _context]
    (not (shield-separator/shield-separator? {:type element-type}))))
