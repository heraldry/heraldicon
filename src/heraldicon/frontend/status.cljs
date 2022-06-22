(ns heraldicon.frontend.status
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn loading []
  [:div [tr :string.miscellaneous/loading]])

(defn not-found []
  [:div [tr :string.miscellaneous/not-found]])

(defn error-display []
  [:div [tr :string.miscellaneous/error]])

(defn default [subscription on-done & {:keys [on-error]}]
  (let [{:keys [status error] :as result} @subscription]
    (case status
      :done [on-done result]
      :error (if on-error
               [on-error error]
               [error-display])
      [loading])))
