(ns heraldicon.reader.blazonry.transform.ordinary-group
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first]]))

(def ^:private max-ordinary-group-amount 20)

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount (transform-first #{:amount} nodes)
        ordinary (transform-first #{:ordinary} nodes)]
    (vec (repeat (-> amount
                     (or 1)
                     (max 1)
                     (min max-ordinary-group-amount)) ordinary))))
