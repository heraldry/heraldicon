(ns heraldicon.reader.blazonry.transform.field
  (:require
   [heraldicon.reader.blazonry.transform.field.partition] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.field.plain] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child filter-nodes]]))

(defmethod ast->hdn :component [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :components [[_ & nodes]]
  (->> nodes
       (filter-nodes #{:component})
       (mapcat ast->hdn)
       vec))

(defmethod ast->hdn :field [[_ & nodes]]
  (if-let [nested-field (get-child #{:field} nodes)]
    ;; for fields inside parentheses
    (ast->hdn nested-field)
    (let [field (ast->hdn (get-child #{:variation} nodes))
          component (some-> (get-child #{:component} nodes) ast->hdn)
          components (some-> (get-child #{:components} nodes) ast->hdn)
          components (vec (concat component components))]
      (cond-> field
        (seq components) (assoc :components components)))))

(defmethod ast->hdn :variation [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :blazon [[_ node]]
  (ast->hdn node))
