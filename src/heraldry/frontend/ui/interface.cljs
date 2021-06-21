(ns heraldry.frontend.ui.interface
  (:require [clojure.string :as s]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.type") t
      (s/starts-with? ts ":heraldry.field") :heraldry.type/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.type/ordinary
      (s/starts-with? ts ":heraldry.charge") :heraldry.type/charge
      :else :heraldry.type/unknown)))

(defn effective-component-type [data]
  (cond
    (map? data) (-> data :type type->component-type)
    (vector? data) :heraldry.type/items
    :else :heraldry.type/unknown))

(defmulti component-form-data (fn [component-data]
                                (effective-component-type component-data)))

(defmulti component-node-data (fn [_path component-data]
                                (effective-component-type component-data)))
