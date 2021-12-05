(ns heraldry.frontend.ui.form.collection-general
  (:require
   [heraldry.context :as c]
   [heraldry.frontend.ui.interface :as ui-interface]
   [heraldry.frontend.validation :as validation]
   [heraldry.gettext :refer [string]]))

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

(defmethod ui-interface/component-node-data :heraldry.component/collection-general [context]
  {:title (string "General")
   :validation (validation/validate-collection-general context)})

(defmethod ui-interface/component-form-data :heraldry.component/collection-general [_context]
  {:form form})
