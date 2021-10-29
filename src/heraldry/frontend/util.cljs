(ns heraldry.frontend.util
  (:require
   [heraldry.config :as config]
   [heraldry.util :as util]
   [reitit.frontend.easy :as reife]))

(defn full-url-for-arms [arms-data]
  (let [version (:version arms-data)
        version (if (zero? version)
                  (:latest-version arms-data)
                  version)
        arms-id (-> arms-data
                    :id
                    util/id-for-url)]
    (str (config/get :heraldry-url) (reife/href :view-arms-by-id-and-version {:id arms-id
                                                                              :version version}))))

(defn full-url-for-charge [charge-data]
  (let [version (:version charge-data)
        version (if (zero? version)
                  (:latest-version charge-data)
                  version)
        charge-id (-> charge-data
                      :id
                      util/id-for-url)]

    (str (config/get :heraldry-url) (reife/href :view-charge-by-id-and-version {:id charge-id
                                                                                :version version}))))
