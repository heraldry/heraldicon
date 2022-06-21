(ns heraldicon.frontend.ribbon
  (:require
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn fetch-ribbon-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [{:keys [status entity]} @(rf/subscribe [::entity-for-rendering/data id version])]
      (when (= status :done)
        entity))
    (log/error "error fetching ribbon data, invalid variant:" variant)))
