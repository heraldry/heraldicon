(ns heraldicon.frontend.component.shield-separator
  (:require
   [heraldicon.frontend.interface :as ui.interface]))

(defmethod ui.interface/component-node-data :heraldry/shield-separator [_context]
  {:title :string.miscellaneous/shield-layer
   :selectable? false})

(defmethod ui.interface/component-form-data :heraldry/shield-separator [_context]
  {})
