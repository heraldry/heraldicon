(ns heraldicon.frontend.charge
  (:require
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.state :as state]
   [taoensso.timbre :as log]))

(defn fetch-charge-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [db-path [:charge-data variant]
          [status charge-data] (state/async-fetch-data db-path variant #(api/fetch-charge-for-rendering id version))]
      (when (= status :done)
        charge-data))
    (log/error "error fetching charge data, invalid variant:" variant)))
