(ns heraldry.collection.options
  (:require [heraldry.frontend.ui.interface :as interface]))

(def default-options
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label "Columns"}}})

(defmethod interface/component-options :collection [_data _path]
  default-options)
