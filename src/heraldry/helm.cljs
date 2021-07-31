(ns heraldry.helm
  (:require [heraldry.interface :as interface]))

(def default-options
  {})

(defn options [data]
  default-options)

(defmethod interface/component-options :heraldry.component/helm [_path data]
  (options data))
