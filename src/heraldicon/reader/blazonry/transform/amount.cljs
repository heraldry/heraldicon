(ns heraldicon.reader.blazonry.transform.amount
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn]]
   [heraldicon.util.number :as number]))

(defmethod ast->hdn :amount [[_ node]]
  (ast->hdn node))

(defmethod ast->hdn :A [_]
  1)

(defmethod ast->hdn :number/NUMBER [[_ number-string]]
  (js/parseInt number-string))

(defmethod ast->hdn :number-word [node]
  (->> node
       (tree-seq (some-fn map? vector? seq?) seq)
       (keep (fn [node]
               (when (and (vector? node)
                          (= (count node) 2)
                          (-> node second string?))
                 (second node))))
       (map (fn [s]
              (or (number/from-string s) 0)))
       (reduce +)))
