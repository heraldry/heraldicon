(ns heraldicon.frontend.charge
  (:require
   [heraldicon.frontend.entity.form :as form]
   [heraldicon.frontend.library.charge.details :as-alias charge.details]
   [heraldicon.frontend.repository.entity-for-rendering :as entity-for-rendering]
   [re-frame.core :as rf]
   [taoensso.timbre :as log]))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (cond
    (= id :form-data-original) {:status :done
                                :entity @(rf/subscribe [::charge.details/original-charge-data])
                                :path (form/data-path :heraldicon.entity.type/charge)}
    (= id :form-data) (let [path (form/data-path :heraldicon.entity.type/charge)]
                        {:status :done
                         :entity @(rf/subscribe [:get path])
                         :path path})
    id @(rf/subscribe [::entity-for-rendering/data id version])
    :else (log/error "error fetching charge data, invalid variant:" variant)))
