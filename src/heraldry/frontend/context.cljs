(ns heraldry.frontend.context
  (:require
   [heraldry.frontend.charge :as charge]))

(def default
  {:load-charge-data charge/fetch-charge-data
   ;; TODO: re-write this feature: allowing highlighting/selecting components in the preview
   :fn-component-selected? nil
   :fn-select-component nil})
