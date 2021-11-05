(ns heraldry.helm
  (:require
   [heraldry.interface :as interface]))

(def default-helm-options
  {})

(defn helm-options [data]
  default-helm-options)

(defmethod interface/component-options :heraldry.component/helm [context]
  (helm-options (interface/get-raw-data context)))

(def default-helms-options
  {})

(defn helms-options [data]
  default-helms-options)

(defmethod interface/component-options :heraldry.component/helms [context]
  (helms-options (interface/get-raw-data context)))
