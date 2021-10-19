(ns heraldry.compartment
  (:require [heraldry.interface :as interface]))

(def default-options
  {})

(defmethod interface/component-options :heraldry.component/compartment [_path _data]
  default-options)
