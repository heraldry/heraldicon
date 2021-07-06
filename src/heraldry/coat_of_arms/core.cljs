(ns heraldry.coat-of-arms.core
  (:require [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.frontend.ui.interface :as interface]))

(def default-options
  {:escutcheon {:type :choice
                :choices (vec (drop 1 escutcheon/choices))
                :default :heater
                :ui {:label "Default escutcheon"
                     :form-type :escutcheon-select}}})

(defn options [_coat-of-arms]
  default-options)

(defmethod interface/component-options :coat-of-arms [data _path]
  (options data))
