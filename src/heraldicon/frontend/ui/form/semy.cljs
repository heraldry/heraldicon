(ns heraldicon.frontend.ui.form.semy
  (:require
   [heraldicon.context :as c]
   [heraldicon.frontend.ui.interface :as ui.interface]
   [heraldicon.heraldry.charge.options :as charge.options]
   [heraldicon.localization.string :as string]))

(defn- form [context]
  (ui.interface/form-elements
   context
   [:anchor
    :layout
    :rectangular?
    :manual-blazon]))

(defmethod ui.interface/component-node-data :heraldry/semy [context]
  (let [charge-context (c/++ context :charge)]
    {:title (string/str-tr :string.miscellaneous/semy-of
                           " "
                           (charge.options/title charge-context))
     :nodes [{:context charge-context}]}))

(defmethod ui.interface/component-form-data :heraldry/semy [_context]
  {:form form})
