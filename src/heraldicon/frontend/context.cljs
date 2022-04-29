(ns heraldicon.frontend.context
  (:require
   [heraldicon.frontend.charge :as charge]))

(def default
  {:load-charge-data charge/fetch-charge-data
   ;; TODO: re-write this feature: allowing highlighting/selecting components in the preview
   :component-selected-fn? nil
   :select-component-fn nil})
