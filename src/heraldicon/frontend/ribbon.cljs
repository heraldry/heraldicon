(ns heraldicon.frontend.ribbon
  (:require
   [heraldicon.frontend.api :as api]
   [heraldicon.frontend.state :as state]
   [taoensso.timbre :as log]))

(defn fetch-ribbon-data [{:keys [id version] :as variant}]
  (if (and id version)
    (let [[status ribbon-data] (state/async-fetch-data
                                [:ribbon-data variant]
                                variant
                                #(api/fetch-ribbon id version nil))]
      (when (= status :done)
        ribbon-data))
    (log/error "error fetching ribbon data, invalid variant:" variant)))
