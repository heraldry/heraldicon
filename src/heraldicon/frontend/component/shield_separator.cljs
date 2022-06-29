(ns heraldicon.frontend.component.shield-separator
  (:require
   [heraldicon.frontend.component.core :as component]))

(defmethod component/node :heraldry/shield-separator [_context]
  {:title :string.miscellaneous/shield-layer
   :selectable? false})

(defmethod component/form :heraldry/shield-separator [_context]
  {})
