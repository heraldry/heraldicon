(ns heraldry.frontend.ui.shield-separator
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]))


(defmethod ui-interface/component-node-data :heraldry.component/shield-separator [_context]
  {:title {:en "Shield layer"
           :de "Schildebene"}
   :selectable? false})

(defmethod ui-interface/component-form-data :heraldry.component/shield-separator [_context]
  {})
