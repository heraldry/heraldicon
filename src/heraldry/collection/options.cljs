(ns heraldry.collection.options
  (:require
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defmethod interface/options-subscriptions :heraldry.component/collection [_context]
  #{})

(defmethod interface/options :heraldry.component/collection [_context]
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label strings/num-columns}}})
