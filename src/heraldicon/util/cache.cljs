(ns heraldicon.util.cache
  (:refer-clojure :exclude [get memoize]))

(def ^:private store
  (atom {}))

(def ^:private lookup-sentinel (js-obj))

(defn memoize
  "Returns a memoized version of a referentially transparent function. The
  memoized version of the function keeps a cache of the mapping from arguments
  to results and, when calls with the same arguments are repeated often, has
  higher performance at the expense of higher memory use."
  [id f]
  (fn [& args]
    (let [v (get-in @store [id args] lookup-sentinel)]
      (if (identical? v lookup-sentinel)
        (let [ret (apply f args)]
          (swap! store assoc-in [id args] ret)
          ret)
        v))))

(defn reset-memoize! []
  (reset! store {}))

(defrecord Node [key value prev next])

(defn create-node [key value]
  (->Node key (atom value) (atom nil) (atom nil)))

(defrecord LRUCache [capacity map head tail])

(defn lru-cache [capacity]
  (let [head (atom nil)
        tail (atom nil)
        cache-map (atom {})]
    (->LRUCache capacity cache-map head tail)))

(defn remove-node! [cache node]
  (let [prev-node @(:prev node)
        next-node @(:next node)]
    (when prev-node
      (reset! (:next prev-node) next-node))
    (when next-node
      (reset! (:prev next-node) prev-node))
    (when (= node @(:head cache))
      (reset! (:head cache) next-node))
    (when (= node @(:tail cache))
      (reset! (:tail cache) prev-node))))

(defn move-to-head! [cache node]
  (remove-node! cache node)
  (let [current-head @(:head cache)]
    (reset! (:prev node) nil)
    (reset! (:next node) current-head)
    (when current-head
      (reset! (:prev current-head) node))
    (reset! (:head cache) node)
    (when (nil? @(:tail cache))
      (reset! (:tail cache) node))))

(defn add-node! [cache node]
  (move-to-head! cache node)
  (swap! (:map cache) assoc (:key node) node))

(defn evict-lru! [cache]
  (let [tail-node @(:tail cache)]
    (when tail-node
      (remove-node! cache tail-node)
      (swap! (:map cache) dissoc (:key tail-node)))))

(defn get [cache key]
  (let [node (clojure.core/get @(:map cache) key)]
    (if node
      (do
        (move-to-head! cache node)
        @(:value node))
      nil)))

(defn put [cache key value]
  (let [existing-node (clojure.core/get @(:map cache) key)]
    (if existing-node
      (do
        (reset! (:value existing-node) value)
        (move-to-head! cache existing-node))
      (let [new-node (create-node key value)]
        (add-node! cache new-node)
        (when (> (count @(:map cache)) (:capacity cache))
          (evict-lru! cache))))))
