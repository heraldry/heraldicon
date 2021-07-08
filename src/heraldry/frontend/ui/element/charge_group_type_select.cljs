(ns heraldry.frontend.ui.element.charge-group-type-select
  (:require [heraldry.frontend.ui.element.radio-select :as radio-select]
            [heraldry.frontend.ui.interface :as interface]
            [re-frame.core :as rf]))

(defmethod interface/form-element :charge-group-type-select [path _]
  [radio-select/radio-select path
   :on-change #(rf/dispatch [:change-charge-group-type (vec (drop-last path)) %])])
