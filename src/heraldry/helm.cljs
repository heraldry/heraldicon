(ns heraldry.helm
  (:require
   [heraldry.interface :as interface]))

(defmethod interface/component-options :heraldry.component/helm [_context]
  {})

(defmethod interface/component-options :heraldry.component/helms [_context]
  {})
