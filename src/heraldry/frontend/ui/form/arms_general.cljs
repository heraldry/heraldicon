(ns heraldry.frontend.ui.form.arms-general
  (:require
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.strings :as strings]
   [re-frame.core :as rf]))

(defn form [context]
  [:<>
   (ui-interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :tags])

   [:div {:style {:height "1.5em"}}]])

(defmethod ui-interface/component-node-data :heraldry.component/arms-general [context]
  {:title strings/general
   :validation @(rf/subscribe [:validate-arms-general context])})

(defmethod ui-interface/component-form-data :heraldry.component/arms-general [_context]
  {:form form})
