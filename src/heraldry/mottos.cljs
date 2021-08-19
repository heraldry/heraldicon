(ns heraldry.mottos
  (:require [heraldry.interface :as interface]))

(def default-options
  {})

(defn options [data]
  default-options)

(defmethod interface/component-options :heraldry.component/mottos [_path data]
  (options data))
