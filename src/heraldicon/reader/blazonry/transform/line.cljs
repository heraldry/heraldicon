(ns heraldicon.reader.blazonry.transform.line
  (:require
   [clojure.string :as s]
   [heraldicon.reader.blazonry.transform.fimbriation :refer [add-fimbriation]]
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first transform-all]]))

(defmethod ast->hdn :line-type [[_ node]]
  (let [raw-type (-> node
                     first
                     name
                     s/lower-case
                     keyword)]
    (get {:rayonny :rayonny-flaming}
         raw-type raw-type)))

(defmethod ast->hdn :line [[_ & nodes]]
  (let [line-type (transform-first #{:line-type} nodes)]
    (-> nil
        (cond->
          line-type (assoc :type line-type))
        (add-fimbriation nodes))))

(defn add-lines [hdn nodes]
  (let [[line
         opposite-line
         extra-line] (transform-all #{:line} nodes)]
    (cond-> hdn
      line (assoc :line line)
      opposite-line (assoc :opposite-line opposite-line)
      extra-line (assoc :extra-line extra-line))))
