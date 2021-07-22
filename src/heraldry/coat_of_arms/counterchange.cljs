(ns heraldry.coat-of-arms.counterchange)

(defn collect-tinctures [field {:keys [tincture-mapping]}]
  (->> field
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) (fn [data]
                                  (if (map? data)
                                    ;; look in this order to really find the most important two tinctures
                                    (concat (-> data :fields)
                                            [(-> data :field)]
                                            (-> data :components)
                                            (-> data :charges))
                                    (seq data))))

       (filter #(and (map? %)
                     (-> % :type (= :heraldry.field.type/plain))
                     (-> % :tincture)))
       (map :tincture)
       (map #(get tincture-mapping % %))
       distinct))

(defn get-counterchange-tinctures [data context]
  (-> data
      (collect-tinctures context)
      (->> (take 2))))
