(ns heraldicon.frontend.status
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn loading []
  [:div [tr :string.miscellaneous/loading]])

(defn not-found []
  [:div [tr :string.miscellaneous/not-found]])
