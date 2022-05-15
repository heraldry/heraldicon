(ns heraldicon.heraldry.component
  (:require
   [clojure.string :as s]))

(defn effective-type [path raw-type]
  (when (some-> raw-type
                str
                (s/starts-with? ":heraldry.charge.type/"))
    (derive raw-type :heraldry.charge/type))
  (cond
    (-> path last (= :helms)) :heraldry/helms
    (-> path last (= :ornaments)) :heraldry/ornaments
    ;; TODO: needed while charge data uses :type as well
    (and (keyword? raw-type)
         (namespace raw-type)) raw-type
    :else nil))
