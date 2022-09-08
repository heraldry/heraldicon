(ns heraldicon.frontend.charge
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.library.charge.details :as-alias charge.details]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (cond
    (= id :form-data-original) @(rf/subscribe [::charge.details/original-charge-data])
    (= id :form-data) @(rf/subscribe [:get (form/data-path :heraldicon.entity.type/charge)])
    id (let [{:keys [status entity]} @(rf/subscribe [::entity-for-rendering/data id version])]
         (when (= status :done)
           entity))
    :else (log/error "error fetching charge data, invalid variant:" variant)))
