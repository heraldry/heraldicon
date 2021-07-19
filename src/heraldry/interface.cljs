(ns heraldry.interface
  (:require [clojure.string :as s]))

(defmulti component-options
  (fn [data path]
    (cond
      (-> path last
          (= :arms-form)) :heraldry.options/arms-general
      (-> path last
          (= :charge-form)) :heraldry.options/charge-general
      (-> path last
          (= :collection-form)) :heraldry.options/collection-general
      (-> path last
          (= :collection)) :heraldry.options/collection
      (-> path drop-last
          (->> (take-last 2))
          (= [:collection :elements])) :heraldry.options/collection-element
      (-> path last
          (= :render-options)) :heraldry.options/render-options
      (-> path last
          (= :coat-of-arms)) :heraldry.options/coat-of-arms
      :else (let [ts (-> data :type str)]
              (cond
                (s/starts-with? ts ":heraldry.field") :heraldry.options/field
                (s/starts-with? ts ":heraldry.ordinary") :heraldry.options/ordinary
                (s/starts-with? ts ":heraldry.component/charge-group-strip") :heraldry.options/charge-group-strip
                (s/starts-with? ts ":heraldry.charge-group") :heraldry.options/charge-group
                (s/starts-with? ts ":heraldry.charge") :heraldry.options/charge
                (s/starts-with? ts ":heraldry.component/semy") :heraldry.options/semy
                :else nil)))))

(defmethod component-options nil [_data _path]
  nil)
