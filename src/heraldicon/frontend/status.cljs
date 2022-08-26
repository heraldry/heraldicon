(ns heraldicon.frontend.status
  (:require
   [heraldicon.frontend.language :refer [tr]]))

(defn loading []
  [:div [tr :string.miscellaneous/loading]])

(defn not-found []
  [:div [tr :string.miscellaneous/not-found]])

(defn error-display [_error]
  [:div [tr :string.miscellaneous/error]])

(defn default [subscription on-done & {:keys [on-error
                                              on-default]
                                       :or {on-error error-display
                                            on-default loading}}]
  (let [{:keys [status error] :as result} @subscription]
    (case status
      :done [on-done result]
      :error [on-error error]
      [on-default])))
