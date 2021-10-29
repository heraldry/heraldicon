(ns heraldry.helm
  (:require
   [heraldry.interface :as interface]))

(def default-helm-options
  {})

(defn helm-options [data]
  default-helm-options)

(defmethod interface/component-options :heraldry.component/helm [_path data]
  (helm-options data))

(def default-helms-options
  {})

(defn helms-options [data]
  default-helms-options)

(defmethod interface/component-options :heraldry.component/helms [_path data]
  (helms-options data))
