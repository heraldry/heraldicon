(ns heraldicon.entity.arms
  (:require
   [heraldicon.config :as config]
   [heraldicon.entity.id :as id]))

(defn short-url [arms-data & {:keys [without-version?]}]
  (if (= (config/get :stage) "prod")
    (let [{:keys [id version latest-version]} arms-data
          link-version (if (zero? version)
                         latest-version
                         version)]
      (when (and id version)
        (if without-version?
          (str "https://coa.to/" (id/for-url id))
          (str "https://coa.to/" (id/for-url id) "/" link-version))))
    "https://dev"))
