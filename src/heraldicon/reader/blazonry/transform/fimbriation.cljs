(ns heraldicon.reader.blazonry.transform.fimbriation
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn filter-nodes transform-first]]))

(defmethod ast->hdn :fimbriation [[_ & nodes]]
  (let [[tincture-1
         tincture-2] (->> nodes
                          (filter-nodes #{:tincture})
                          (take 2)
                          (mapv ast->hdn))]
    (if-not tincture-2
      {:mode :single
       :tincture-1 tincture-1}
      {:mode :double
       :tincture-1 tincture-1
       :tincture-2 tincture-2})))

(defn add-fimbriation [hdn nodes & {:keys [line-fimbriation?]}]
  (let [fimbriation (transform-first #{:fimbriation} nodes)
        path (if line-fimbriation?
               [:line :fimbriation]
               [:fimbriation])]
    (cond-> hdn
      fimbriation (assoc-in path fimbriation))))
