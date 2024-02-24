(ns heraldicon.frontend.component.drag
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.element :as component.element]
   [re-frame.core :as rf]))

(defmulti drop-allowed?
  (fn [drag-node drop-node]
    [(:type drag-node) (:type drop-node)]))

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

(defmethod drop-allowed? :default
  [_drag-node _drop-node]
  false)

(defmethod drop-allowed? [:heraldicon.entity.collection/element
                          :heraldicon.entity.collection/data]
  [_drag-node _drop-node]
  true)

(defmethod drop-allowed? [:heraldicon/charge-type
                          :heraldicon/charge-type]
  [_drag-node _drop-node]
  true)

(defmulti drop-inside-target-context
  (fn [drag-node drop-node]
    [(:type drag-node) (:type drop-node)]))

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
    (rf/dispatch [::component.element/move-general drag-node-context target-context])))

(defmethod drop-inside-target-context [:heraldicon/charge-type
                                       :heraldicon/charge-type]
  [_drag-node drop-node]
  (c/++ (:context drop-node) :types component.element/APPEND-INDEX))
