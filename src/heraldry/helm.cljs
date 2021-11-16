(ns heraldry.helm
  (:require
   [heraldry.interface :as interface]))

(defmethod interface/options :heraldry.component/helm [_context]
  {})

(defmethod interface/options :heraldry.component/helms [_context]
  {})
