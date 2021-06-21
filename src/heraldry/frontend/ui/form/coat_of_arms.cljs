(ns heraldry.frontend.ui.form.coat-of-arms
  (:require [heraldry.coat-of-arms.core :as coat-of-arms]
            [heraldry.frontend.ui.interface :as interface]
            [heraldry.frontend.ui.option :as option]))

(defn form [path {:keys [options]}]
  [:<>
   (for [option [:escutcheon]]
     ^{:key option} [option/form (conj path option) (get options option)])])

(defmethod interface/component-node-data :heraldry.type/coat-of-arms [path _component-data]
  {:title "coat of arms"
   :nodes [{:path (conj path :field)}
           {:title "components"
            :path (conj path :field :components)}]})

(defmethod interface/component-form-data :heraldry.type/coat-of-arms [component-data]
  {:form form
   :form-args {:options (coat-of-arms/options component-data)}})
