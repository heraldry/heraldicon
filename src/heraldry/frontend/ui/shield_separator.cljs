(ns heraldry.frontend.ui.shield-separator
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.gettext :refer [string]]))


(defmethod ui-interface/component-node-data :heraldry.component/shield-separator [_context]
  {:title (string "Shield layer")
   :selectable? false})

(defmethod ui-interface/component-form-data :heraldry.component/shield-separator [_context]
  {})
