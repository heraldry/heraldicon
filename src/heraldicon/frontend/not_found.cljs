(ns heraldicon.frontend.not-found
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn not-found []
  [:div [tr :string.miscellaneous/not-found]])
