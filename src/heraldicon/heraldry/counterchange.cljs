(ns heraldicon.heraldry.counterchange
  (:require
   [heraldicon.context :as c]))

(defn- breadth-first-tree-seq [branch? children tree]
  (loop [result []
         queue (conj [] tree)]
    (if (seq queue)
      (let [next-node (first queue)]
        (if (branch? next-node)
          (recur (conj result next-node)
                 (into (vec (next queue)) (children next-node)))
          (recur (conj result next-node)
                 (vec (next queue)))))
      result)))

(defn- collect-tinctures [field context]
  (let [tincture-mapping (c/tincture-mapping context)]
    (->> field
         (breadth-first-tree-seq #(or (map? %)
                                      (vector? %)
                                      (seq? %))
                                 (fn [data]
                                   (if (map? data)
                       ;; look in this order to find the most important two tinctures
                                     (concat (:fields data)
                                             [(:field data)]
                                             [(:charge data)]
                                             (:components data)
                                             (:charges data))
                                     (seq data))))

         (filter #(and (map? %)
                       (-> % :type (= :heraldry.field.type/plain))
                       (:tincture %)))
         (map :tincture)
         (map #(get tincture-mapping % %))
         distinct)))

(defn get-tinctures [data context]
  (-> data
      (collect-tinctures context)
      (->> (take 2))))
