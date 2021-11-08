(ns heraldry.component
  (:require [clojure.string :as s]))

(defn type->component-type [t]
  (let [ts (str t)]
    (cond
      (s/starts-with? ts ":heraldry.component") t
      (s/starts-with? ts ":heraldry.field") :heraldry.component/field
      (s/starts-with? ts ":heraldry.ordinary") :heraldry.component/ordinary
      (s/starts-with? ts ":heraldry.charge-group") :heraldry.component/charge-group
      (s/starts-with? ts ":heraldry.charge") :heraldry.component/charge
      (s/starts-with? ts ":heraldry.ribbon.segment") :heraldry.component/ribbon-segment
      (s/starts-with? ts ":heraldry.motto") :heraldry.component/motto
      :else nil)))

(defn effective-type [path raw-type]
  (cond
    (-> path last (= :arms-form)) :heraldry.component/arms-general
    (-> path last #{:charge-form
                    :charge-data}) :heraldry.component/charge-general
    (-> path last (= :collection-form)) :heraldry.component/collection-general
    (-> path last (= :collection)) :heraldry.component/collection
    (->> path drop-last (take-last 2) (= [:collection :elements])) :heraldry.component/collection-element
    (-> path last (= :render-options)) :heraldry.component/render-options
    (-> path last (= :helms)) :heraldry.component/helms
    (-> path last (= :ribbon-form)) :heraldry.component/ribbon-general
    (-> path last (= :coat-of-arms)) :heraldry.component/coat-of-arms
    (-> path last (= :ornaments)) :heraldry.component/ornaments
    (keyword? raw-type) (type->component-type raw-type)
    (and (-> path last keyword?)
         (-> path last name (s/starts-with? "cottise"))) :heraldry.component/cottise
    :else nil))
