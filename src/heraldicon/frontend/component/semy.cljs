(ns heraldicon.frontend.component.semy
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.component.core :as component]
   [heraldicon.frontend.element.core :as element]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.localization.string :as string]))

(defn- form [context]
  (element/elements
   context
   [:adapt-to-ordinaries?
    :anchor
    :layout
    :rectangular?
    :manual-blazon]))

(defmethod component/node :heraldry/semy [context]
  (let [charge-context (c/++ context :charge)]
    {:title (string/str-tr :string.miscellaneous/semy-of
                           " "
                           (charge.options/title charge-context))
     :nodes [{:context charge-context}]}))

(defmethod component/form :heraldry/semy [_context]
  form)
