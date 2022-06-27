(ns heraldicon.reader.blazonry.transform.ordinary-group
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child]]))

(def ^:private max-ordinary-group-amount 20)

(defmethod ast->hdn :ordinary-group [[_ & nodes]]
  (let [amount-node (get-child #{:amount} nodes)
        amount (if amount-node
                 (ast->hdn amount-node)
                 1)
        ordinary (ast->hdn (get-child #{:ordinary} nodes))]
    (vec (repeat (-> amount
                     (max 1)
                     (min max-ordinary-group-amount)) ordinary))))
