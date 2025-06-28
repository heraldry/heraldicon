(ns heraldicon.frontend.repository.core
  (:require
   [re-frame.core :as rf]))

(def db-path-base
  [:repository])

(defn async-query-data [path load-fn & {:keys [on-loaded
                                               data-stale?]}]
  (let [data @(rf/subscribe [:get path])]
    (if (and data
             (or (not data-stale?)
                 (not (data-stale? data))))
      data
      (do
        (load-fn :on-loaded on-loaded)
        {:status :loading}))))

(rf/reg-event-db ::session-change
  (fn [db _]
    (assoc-in db db-path-base nil)))
