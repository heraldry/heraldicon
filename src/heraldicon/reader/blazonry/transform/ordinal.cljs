(ns heraldicon.reader.blazonry.transform.ordinal
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn]]
   [heraldicon.util.number :as number]))

(defmethod ast->hdn :ordinal [[_ & nodes]]
  (->> nodes
       (tree-seq (some-fn map? vector? seq?) seq)
       (filter string?)
       first
       number/ordinal-from-string))
