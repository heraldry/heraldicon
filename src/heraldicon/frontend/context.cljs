(ns heraldicon.frontend.context
  (:require
   [heraldicon.frontend.charge :as charge]))

(def default
  {:load-charge-data charge/fetch-charge-data})
