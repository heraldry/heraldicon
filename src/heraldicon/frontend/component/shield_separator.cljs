(ns heraldicon.frontend.component.shield-separator
  (:require
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.component.drag :as drag]))

(defmethod component/node :heraldry/shield-separator [_context]
  {:title :string.miscellaneous/shield-layer
   :selectable? false
   :draggable? true
   :drop-options-fn drag/drop-options
   :drop-fn drag/drop-fn})

(defmethod component/form :heraldry/shield-separator [_context]
  {})
