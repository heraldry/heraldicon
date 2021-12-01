(ns heraldry.collection.options
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/collection [_context]
  #{})

(defmethod interface/options :heraldry.component/collection [_context]
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label (string "Number of columns")}}})
