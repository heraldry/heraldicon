(ns heraldry.frontend.context
  (:require [heraldry.frontend.charge-map :as charge-map]))

(def default
  {:load-charge-data charge-map/fetch-charge-data})
