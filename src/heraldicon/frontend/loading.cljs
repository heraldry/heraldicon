(ns heraldicon.frontend.loading
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn loading []
  [:div [tr :string.miscellaneous/loading]])
