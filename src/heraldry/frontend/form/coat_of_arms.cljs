(ns heraldry.frontend.form.coat-of-arms
  (:require [heraldry.frontend.form.element :as element]
            [heraldry.frontend.form.escutcheon :as escutcheon]
            [heraldry.frontend.form.field :as field]))

(defn form [db-path]
  [element/component db-path :coat-of-arms "Coat of Arms" nil
   [escutcheon/form (conj db-path :escutcheon) "Default Escutcheon" :label-width "11em"]
   [field/form (conj db-path :field)]])
