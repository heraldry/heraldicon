(ns heraldicon.frontend.ui.form.entity.core
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

   (let [data-context (c/++ context :data)]
     (when-let [data-form (:form (ui.interface/component-form-data data-context))]
       [:<>
        [:div {:style {:height "1.5em"}}]
        [data-form data-context]]))])

(defmethod ui.interface/component-node-data :heraldicon/entity [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod ui.interface/component-form-data :heraldicon/entity [_context]
  {:form form})
