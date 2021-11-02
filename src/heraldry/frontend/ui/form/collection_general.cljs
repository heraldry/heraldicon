(ns heraldry.frontend.ui.form.collection-general
  (:require
   [heraldry.context :as c]
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

   [:div {:style {:height "1.5em"}}]

   [ui-interface/form-element (c/++ context :font)]])

(defmethod ui-interface/component-node-data :heraldry.component/collection-general [{:keys [path]}]
  {:title strings/general
   :validation @(rf/subscribe [:validate-collection-general path])})

(defmethod ui-interface/component-form-data :heraldry.component/collection-general [_context]
  {:form form})
