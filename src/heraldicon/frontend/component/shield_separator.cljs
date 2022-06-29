(ns heraldicon.frontend.component.shield-separator
  (:require
   [heraldicon.frontend.component.core :as component]))

(defmethod component/node-data :heraldry/shield-separator [_context]
  {:title :string.miscellaneous/shield-layer
   :selectable? false})

(defmethod component/form-data :heraldry/shield-separator [_context]
  {})
