(ns heraldicon.heraldry.counterchange)

(defn- collect-tinctures [field {:keys [tincture-mapping]}]
  (->> field
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) (fn [data]
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
       distinct))

(defn get-tinctures [data context]
  (-> data
      (collect-tinctures context)
      (->> (take 2))))
