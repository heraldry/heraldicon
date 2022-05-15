(ns heraldicon.heraldry.component
  (:require
   [clojure.string :as s]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.ordinary") :heraldry/ordinary
      (s/starts-with? ts ":heraldry.charge.") :heraldry/charge
      (s/starts-with? ts ":heraldry") t
      (s/starts-with? ts ":heraldicon") t
      :else nil)))

(defn effective-type [path raw-type]
  (cond
    (-> path last (= :helms)) :heraldry/helms
    (-> path last (= :ornaments)) :heraldry/ornaments
    (keyword? raw-type) (type->component-type raw-type)
    :else nil))
