(ns heraldicon.frontend.ui.form.entity.collection
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.form.entity.collection.data :as collection.data]
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

   [collection.data/form (c/++ context :data)]])

(defmethod ui.interface/component-node-data :heraldicon/collection [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon/collection [_context]
  {:form form})
