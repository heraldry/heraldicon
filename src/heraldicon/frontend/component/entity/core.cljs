(ns heraldicon.frontend.component.entity.core
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.frontend.validation :as validation]))

(defn- form [context]
  [:<>
   (element/elements
    context
    [:name
     :attribution
     :access
     :metadata
     :tags])

   (let [data-context (c/++ context :data)]
     (when-let [data-form (component/form data-context)]
       [:<>
        [:div {:style {:height "1.5em"}}]
        [data-form data-context]]))])

(defmethod component/node-data :heraldicon/entity [context]
  {:title :string.miscellaneous/general
   :validation (validation/validate-entity context)})

(defmethod component/form :heraldicon/entity [_context]
  form)
