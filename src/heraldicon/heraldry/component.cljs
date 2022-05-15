(ns heraldicon.heraldry.component
  (:require
   [clojure.string :as s]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.field") :heraldry/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry/charge-group
      (s/starts-with? ts ":heraldry.charge") :heraldry/charge
      (s/starts-with? ts ":heraldry.ribbon.segment") :heraldry/ribbon-segment
      (s/starts-with? ts ":heraldry.motto") :heraldry/motto
      (s/starts-with? ts ":heraldry") t
      :else nil)))

(defn effective-type [path raw-type]
  (cond
    (-> path last (= :arms-form)) :heraldry/arms-general
    (-> path last #{:charge-form
                    :charge-data}) :heraldry/charge-general
    (-> path last (= :collection-form)) :heraldry/collection-general
    (-> path last (= :collection)) :heraldry/collection
    (->> path drop-last (take-last 2) (= [:collection :elements])) :heraldry/collection-element
    (-> path last (= :render-options)) :heraldry/render-options
    (-> path last (= :helms)) :heraldry/helms
    (-> path last (= :ribbon-form)) :heraldry/ribbon-general
    (-> path last (= :coat-of-arms)) :heraldry/coat-of-arms
    (-> path last (= :ornaments)) :heraldry/ornaments
    (keyword? raw-type) (type->component-type raw-type)
    :else nil))
