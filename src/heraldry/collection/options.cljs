(ns heraldry.collection.options
  (:require
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(def default-options
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label strings/num-columns}}})

(defmethod interface/component-options :heraldry.component/collection [_path _data]
  default-options)
