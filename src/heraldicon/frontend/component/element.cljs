(ns heraldicon.frontend.component.element
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.tree :as tree]
   [heraldicon.frontend.element.submenu :as submenu]
   [heraldicon.frontend.macros :as macros]
   [heraldicon.heraldry.component :as component]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [re-frame.core :as rf]))

(def APPEND-INDEX 10000)

(macros/reg-event-fx ::add
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context} value {:keys [post-fn selected-element-path-fn]}]]
    (let [new-db (cond-> (update-in db path (fn [elements]
                                              (vec (conj elements value))))
                   post-fn (post-fn path))
          elements (get-in db path)
          new-element-path (conj path (-> elements count dec))
          new-element-path (if selected-element-path-fn
                             (selected-element-path-fn new-element-path (last elements) elements)
                             new-element-path)
          added-type (component/effective-type (:type value))
          parent-type (get-in db (-> context
                                     c/--
                                     (c/++ :type)
                                     :path))]
      {:db (cond-> new-db
             (or (isa? parent-type
                       :heraldry/helm)
                 (isa? parent-type
                       :heraldry/ornaments)) (shield-separator/add-or-remove-shield-separator path))
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

(macros/reg-event-fx ::move
  (fn [{:keys [db]} [_
                     {value-path :path}
                     {target-path :path}
                     {:keys [no-select? post-fn]}]]
    (let [[new-db value] (remove-element db value-path)
          adjusted-target-path (adjust-path-after-removal target-path value-path)
          [new-db new-value-path] (insert-element new-db adjusted-target-path value)]
      {:db (-> new-db
               (tree/element-removed value-path)
               (tree/element-inserted new-value-path)
               (cond->
                 post-fn (post-fn value-path target-path)
                 (not no-select?) (tree/select-node new-value-path true)))})))

(macros/reg-event-fx ::remove
  (fn [{:keys [db]} [_ {:keys [path]
                        :as context} {:keys [post-fn]}]]
    (let [[new-db _value] (remove-element db path)
          parent-type (get-in new-db (-> context
                                         (c/-- 2)
                                         (c/++ :type)
                                         :path))]
      {:db (-> (cond-> new-db
                 (or (isa? parent-type
                           :heraldry/helm)
                     (isa? parent-type
                           :heraldry/ornaments)) (shield-separator/add-or-remove-shield-separator path)
                 post-fn (post-fn path))
               (tree/element-removed path))})))
