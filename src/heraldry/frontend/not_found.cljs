(ns heraldry.frontend.not-found
  (:require
   [heraldry.frontend.language :refer [tr]]))

(defn not-found []
  [:div [tr :string.miscellaneous/not-found]])
