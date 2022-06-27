(ns heraldicon.reader.blazonry.transform.shared
  (:require
   [taoensso.timbre :as log]))

(defmulti ast->hdn first)

(defmethod ast->hdn :default [ast]
  (log/debug :ast->hdn-error ast)
  ast)

(defn- type? [type-fn]
  #(-> % first type-fn))

(defn filter-nodes [type-fn nodes]
  (filter (type? type-fn) nodes))

(defn get-child [type-fn nodes]
  (first (filter-nodes type-fn nodes)))

(defn transform-first [type-fn nodes]
  (some-> (get-child type-fn nodes)
          ast->hdn))
