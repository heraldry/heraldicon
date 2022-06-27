(ns heraldicon.reader.blazonry.transform.cottising
  (:require
   [clojure.set :as set]
   [heraldicon.heraldry.default :as default]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.line :refer [add-lines]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first filter-nodes]]))

(defmethod ast->hdn :cottise [[_ & nodes]]
  (let [field (transform-first #{:field} nodes)]
    (-> default/cottise
        (assoc :field field)
        (add-lines nodes)
        (add-fimbriation nodes :line-fimbriation? true))))

(defmethod ast->hdn :cottising [[_ & nodes]]
  (let [[cottise-1
         cottise-2] (->> nodes
                         (filter-nodes #{:cottise})
                         (map ast->hdn))
        double-node (get-child #{:DOUBLY} nodes)
        cottise-2 (or cottise-2
                      (when double-node
                        cottise-1))]
    (-> {:cottise-1 cottise-1}
        (cond->
          cottise-2 (assoc :cottise-2 cottise-2))
        (add-lines nodes))))

(defn add-cottising [hdn nodes]
  (let [[main
         opposite
         extra] (->> nodes
                     (filter-nodes #{:cottising})
                     (map ast->hdn))
        ordinary-type (-> hdn :type name keyword)
        opposite (or opposite
                     (when (#{:fess
                              :pale
                              :bend
                              :bend-sinister
                              :chevron
                              :pall} ordinary-type)
                       main))
        extra (or extra
                  (when (#{:pall} ordinary-type)
                    main))]
    (cond-> hdn
      main (update :cottising merge main)
      opposite (update :cottising merge (set/rename-keys opposite
                                                         {:cottise-1 :cottise-opposite-1
                                                          :cottise-2 :cottise-opposite-2}))
      extra (update :cottising merge (set/rename-keys extra
                                                      {:cottise-1 :cottise-extra-1
                                                       :cottise-2 :cottise-extra-2})))))
