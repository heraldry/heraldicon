(ns heraldicon.frontend.library.arms.shared
  (:require
   [heraldicon.frontend.context :as context]
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.history.core :as history]
   [heraldicon.frontend.state :as state]))

(def form-id
  :heraldicon.entity/arms)

(def form-db-path
  (form/data-path form-id))

(def saved-data-db-path
  (form/saved-data-path form-id))

(history/register-undoable-path form-db-path)

(defn base-context []
  (assoc
   context/default
   :path form-db-path
   :render-options-path (conj form-db-path :data :achievement :render-options)
   :select-component-fn (fn [event context]
                          (state/dispatch-on-event event [:ui-component-node-select (:path context)]))))
