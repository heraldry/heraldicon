(ns heraldicon.frontend.repository.core
  (:require
   [re-frame.core :as rf]))

(def db-path-base
  [:repository])

(defn async-query-data [path load-fn]
  (let [data @(rf/subscribe [:get path])]
    (if data
      data
      (do
        (load-fn)
        {:status :loading}))))
