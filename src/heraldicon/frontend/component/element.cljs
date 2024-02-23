(ns heraldicon.frontend.component.element
  (:require
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.util.vec :as vec]
   [re-frame.core :as rf]))

(macros/reg-event-fx ::add
  (fn [{:keys [db]} [_ {:keys [path]} value {:keys [post-fn selected-element-path-fn]}]]
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
      {:db (assoc-in db path elements)
       :dispatch-n [[::submenu/close-all]
                    [::tree/select-node (if (isa? added-type :heraldry/helm)
                                          (conj new-element-path :components 1)
                                          new-element-path)
                     true]
                    (cond
                      (isa? added-type :heraldry/helm) [::submenu/open (conj new-element-path :components 1 :type)]
                      (isa? added-type :heraldry/ordinary) [::submenu/open (conj new-element-path :type)]
                      (isa? added-type :heraldry/charge) [::submenu/open (conj new-element-path :type)]
                      (isa? added-type :heraldry/charge-group) [::submenu/open new-element-path]
                      (isa? added-type :heraldry/motto) [::submenu/open (conj new-element-path :ribbon-variant)]
                      (isa? added-type :heraldicon.entity.collection/element) [::submenu/open (conj new-element-path :reference)]
                      :else nil)]})))

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

; TODO: deal with path changes
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

(defn insert-element
  [db target-path value]
  (let [elements-path (vec (drop-last target-path))
        elements (vec (get-in db elements-path))
        index (-> (last target-path)
                  (max 0)
                  (min (count elements)))
        new-elements (vec (concat (subvec elements 0 index)
                                  [value]
                                  (subvec elements index)))]
    [(assoc-in db elements-path new-elements)
     (conj elements-path index)]))

(defn remove-element
  [db path]
  (let [elements-path (vec (drop-last path))
        index (last path)
        value (get-in db path)
        elements (vec (get-in db elements-path))
        new-elements (vec (concat (subvec elements 0 index)
                                  (subvec elements (inc index))))]
    [(assoc-in db elements-path new-elements)
     value]))

(defn adjust-path-after-removal
  [path removed-path]
  ; if the removed path is longer, then its removal can't affected the path
  (if (<= (count removed-path)
          (count path))
    (let [index-pos (dec (count removed-path))
          path-start (take index-pos path)
          removal-index (last removed-path)
          path-index (get path index-pos)]
      (if (and (= path-start (drop-last removed-path))
               (< removal-index path-index))
        (vec (concat path-start [(dec path-index)] (drop (inc index-pos) path)))

        path))

    path))

(macros/reg-event-fx ::move-general
  (fn [{:keys [db]} [_
                     {value-path :path}
                     {target-path :path}]]
    (let [[new-db value] (remove-element db value-path)
          adjusted-target-path (adjust-path-after-removal target-path value-path)
          [new-db new-value-path] (insert-element new-db adjusted-target-path value)]
      {:db (tree/select-node new-db new-value-path true)})))

(macros/reg-event-fx ::remove-general
  (fn [{:keys [db]} [_ {:keys [path]}]]
    (let [[new-db _value] (remove-element db path)]
      {:db new-db})))
