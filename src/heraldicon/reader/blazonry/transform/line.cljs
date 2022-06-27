(ns heraldicon.reader.blazonry.transform.line
  (:require
   [clojure.string :as s]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child filter-nodes]]))

(defmethod ast->hdn :line-type [[_ node]]
  (let [raw-type (-> node
                     first
                     name
                     s/lower-case
                     keyword)]
    (get {:rayonny :rayonny-flaming}
         raw-type raw-type)))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (get-child #{:line-type} nodes)]
    (-> nil
        (cond->
          line-type (assoc :type (ast->hdn line-type)))
        (add-fimbriation nodes))))

(defn add-lines [hdn nodes]
  (let [[line
         opposite-line
         extra-line] (->> nodes
                          (filter-nodes #{:line})
                          (mapv ast->hdn))]
    (cond-> hdn
      line (assoc :line line)
      opposite-line (assoc :opposite-line opposite-line)
      extra-line (assoc :extra-line extra-line))))
