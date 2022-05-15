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

(defmethod ui.interface/component-node-data :heraldicon/collection [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon/collection [_context]
  {:form form})
