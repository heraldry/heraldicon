(ns heraldicon.frontend.charge
  (:require
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if id
    (let [{:keys [status entity]} @(rf/subscribe [::entity-for-rendering/data id version])]
      (when (= status :done)
        entity))
    (log/error "error fetching charge data, invalid variant:" variant)))
