(ns heraldry.helms
  (:require [heraldry.interface :as interface]))

(def default-options
  {})

(defn options [data]
  default-options)

(defmethod interface/component-options :heraldry.component/helms [_path data]
  (options data))
