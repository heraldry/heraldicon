(ns heraldry.frontend.ui.form.arms-general
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.frontend.validation :as validation]))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :metadata
     :tags])

   [:div {:style {:height "1.5em"}}]])

(defmethod ui-interface/component-node-data :heraldry.component/arms-general [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-arms-general context)})

(defmethod ui-interface/component-form-data :heraldry.component/arms-general [_context]
  {:form form})
