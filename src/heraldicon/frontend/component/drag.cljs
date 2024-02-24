(ns heraldicon.frontend.component.drag
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.element :as component.element]
   [re-frame.core :as rf]))

(defn- field-component?
  [type]
  (or (isa? type :heraldry/ordinary)
      (isa? type :heraldry/charge)
      (isa? type :heraldry/charge-group)
      (isa? type :heraldry/semy)))

(defn- drop-allowed?
  [drag-node drop-node]
  (let [drag-type (:type drag-node)
        drop-type (:type drop-node)]
    (or (and (isa? drag-type :heraldicon.entity.collection/element)
             (isa? drop-type :heraldicon.entity.collection/data))

        (and (isa? drag-type :heraldicon/charge-type)
             (isa? drop-type :heraldicon/charge-type))

        (and (isa? drag-type :heraldry/helm)
             (isa? drop-type :heraldry/helms))

        (and (field-component? drag-type)
             (or (isa? drop-type :heraldry/field)
                 (isa? drop-type :heraldry/ordinary)
                 (isa? drop-type :heraldry/charge))))))

(defn- parent-node
  [{:keys [parent-context parent-type]
    :as node}]
  (-> node
      (assoc :context parent-context)
      (assoc :type parent-type)
      (dissoc :parent-context :parent-type)))

(defn inside-own-subtree?
  [drag-path drop-path]
  (= (take (count drag-path) drop-path)
     drag-path))

(defn- sibling?
  [path-1 path-2]
  (= (drop-last path-1)
     (drop-last path-2)))

(defn- allowed-edges
  [drag-path drop-path]
  (when (not= drag-path drop-path)
    (let [current-index (last drag-path)
          new-index (last drop-path)
          siblings? (sibling? drag-path drop-path)]
      (cond-> #{:above :below}
        siblings? (cond->
                    (= new-index
                       (dec current-index)) (disj :below)

                    (= new-index
                       (inc current-index)) (disj :above))))))

(defn drop-options
  [drag-node drop-node]
  (let [drag-path (:path (:context drag-node))
        drop-path (:path (:context drop-node))]
    (when-not (inside-own-subtree? drag-path drop-path)
      (let [inside? (and (not= (:path (:parent-context drag-node))
                               drop-path)
                         (drop-allowed? drag-node drop-node))
            allowed-in-parent? (drop-allowed? drag-node (parent-node drop-node))
            in-collection? (int? (last drop-path))
            allowed-edges (allowed-edges drag-path drop-path)
            above? (and allowed-in-parent?
                        in-collection?
                        (get allowed-edges :above))
            below? (and allowed-in-parent?
                        in-collection?
                        (get allowed-edges :below)
                        (not (:open? drop-node)))]
        (cond-> #{}
          inside? (conj :inside)
          above? (conj :above)
          below? (conj :below))))))

(defn- drop-inside-target-context
  [drag-node drop-node]
  (let [drag-type (:type drag-node)
        drop-type (:type drop-node)]
    (cond
      (and (isa? drag-type :heraldicon/charge-type)
           (isa? drop-type :heraldicon/charge-type)) (c/++ (:context drop-node)
                                                           :types component.element/APPEND-INDEX)

      (and (field-component? drag-type)
           (isa? drop-type :heraldry/field)) (c/++ (:context drop-node)
                                                   :components component.element/APPEND-INDEX)

      (and (field-component? drag-type)
           (or (isa? drop-type :heraldry/ordinary)
               (isa? drop-type :heraldry/charge))) (c/++ (:context drop-node)
                                                         :field :components component.element/APPEND-INDEX)
      :else nil)))

(defn drop-fn
  [drag-node drop-node]
  (let [new-index (last (:path (:context drop-node)))
        drag-node-context (:context drag-node)
        drop-node-context (:context drop-node)
        where (:where drop-node)
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :inside (drop-inside-target-context drag-node drop-node)
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (when target-context
      (rf/dispatch [::component.element/move-general drag-node-context target-context
                    {:no-select? (#{:heraldry/helm} (:type drag-node))}]))))
