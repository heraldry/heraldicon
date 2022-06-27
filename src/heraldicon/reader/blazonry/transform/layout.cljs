(ns heraldicon.reader.blazonry.transform.layout
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn transform-first filter-nodes]]))

(def ^:private max-layout-amount 50)

(defmethod ast->hdn :horizontal-layout [[_ & nodes]]
  {:num-fields-x (min max-layout-amount
                      (transform-first #{:amount} nodes))})

(defmethod ast->hdn :vertical-layout-implicit [[_ & nodes]]
  {:num-fields-y (min max-layout-amount
                      (transform-first #{:amount} nodes))})

(defmethod ast->hdn :vertical-layout-explicit [[_ & nodes]]
  {:num-fields-y (min max-layout-amount
                      (transform-first #{:amount} nodes))})

(defmethod ast->hdn :vertical-layout [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :layout [[_ & nodes]]
  (let [layouts (->> nodes
                     (filter-nodes #{:horizontal-layout
                                     :vertical-layout
                                     :vertical-layout-explicit
                                     :vertical-layout-implicit})
                     (map ast->hdn))]
    (apply merge layouts)))
