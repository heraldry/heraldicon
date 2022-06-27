(ns heraldicon.reader.blazonry.transform.field
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first transform-all]]))

(defmethod ast->hdn :component [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :components [[_ & nodes]]
  (->> (transform-all #{:component} nodes)
       (apply concat)
       vec))

(defmethod ast->hdn :field [[_ & nodes]]
  ;; for fields inside parentheses
  (or (transform-first #{:field} nodes)
      (let [field (transform-first #{:variation} nodes)
            components (vec (concat (transform-first #{:component} nodes)
                                    (transform-first #{:components} nodes)))]
        (cond-> field
          (seq components) (assoc :components components)))))

(defmethod ast->hdn :variation [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :blazon [[_ node]]
  (ast->hdn node))
