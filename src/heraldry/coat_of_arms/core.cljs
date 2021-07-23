(ns heraldry.coat-of-arms.core
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.interface :as interface]))

(def default-options
  {:escutcheon {:type :choice
                :choices (vec (drop 1 escutcheon/choices))
                :default :heater
                :ui {:label "Default escutcheon"
                     :form-type :escutcheon-select}}})

(defn options [_coat-of-arms]
  default-options)

(defmethod interface/component-options :heraldry.options/coat-of-arms [_path data]
  (options data))

(defmethod interface/blazon-component :heraldry.component/coat-of-arms [path context]
  (interface/blazon (conj path :field) (assoc-in context [:blazonry :root?] true)))
