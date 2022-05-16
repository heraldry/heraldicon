(ns heraldicon.frontend.ui.form.entity.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.form.entity.ribbon.data :as ribbon.data]
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

   [ribbon.data/form (c/++ context :data)]])

(defmethod ui.interface/component-node-data :heraldicon/ribbon [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon/ribbon [_context]
  {:form form})
