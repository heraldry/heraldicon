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
  (let [inside? (drop-allowed? drag-node drop-node)
        allowed-in-parent? (drop-allowed? drag-node (parent-node drop-node))
        drag-path (:path (:context drag-node))
        drop-path (:path (:context drop-node))
        in-collection? (int? (last drop-path))
        allowed-edges (allowed-edges drag-path drop-path)
        above? (and allowed-in-parent?
                    in-collection?
                    (get allowed-edges :above))
        below? (and allowed-in-parent?
                    (not (:open? drop-node))
                    in-collection?
                    (get allowed-edges :below))]
    (cond-> #{}
      inside? (conj :inside)
      above? (conj :above)
      below? (conj :below))))

(defmethod drop-allowed? :default
  [_drag-node _drop-node]
  false)

(defmethod drop-allowed? [:heraldicon.entity.collection/element
                          :heraldicon.entity.collection/data]
  [_drag-node _drop-node]
  true)

(defn drop-fn
  [drag-node drop-node]
  (let [new-index (last (:path (:context drop-node)))
        drag-node-context (:context drag-node)
        drop-node-context (:context drop-node)
        where (:where drop-node)
        target-context (case where
                         :above (-> drop-node-context c/-- (c/++ new-index))
                         :below (-> drop-node-context c/-- (c/++ (inc new-index))))]
    (rf/dispatch [::component.element/move-general drag-node-context target-context])))
