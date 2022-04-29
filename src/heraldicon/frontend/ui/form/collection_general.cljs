(ns heraldicon.frontend.ui.form.collection-general
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.frontend.validation :as validation]))

(defn form [context]
  [:<>
   (ui.interface/form-elements
    context
    [:name
     :attribution
     :is-public
     :metadata
     :tags])

   [:div {:style {:height "1.5em"}}]

   [ui.interface/form-element (c/++ context :font)]])

(defmethod ui.interface/component-node-data :heraldry.component/collection-general [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-collection-general context)})

(defmethod ui.interface/component-form-data :heraldry.component/collection-general [_context]
  {:form form})
