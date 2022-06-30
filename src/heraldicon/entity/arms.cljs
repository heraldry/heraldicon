(ns heraldicon.entity.arms
  (:require
   [heraldicon.config :as config]
   [heraldicon.entity.id :as id]))

(defn short-url [arms-data & {:keys [without-version?]}]
  (if (= (config/get :stage) "prod")
    (let [{:keys [id version]} arms-data]
      (when (and id version)
        (if without-version?
          (str "https://coa.to/" (id/for-url id))
          (str "https://coa.to/" (id/for-url id) "/" version))))
    "https://dev"))

(defn charge-variants [data]
  (->> data
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) seq)
       (filter #(and (map? %)
                     (some-> % :type namespace (= "heraldry.charge.type"))
                     (-> % :variant :id)))
       (map :variant)
       set))

(defn used-ribbons [data]
  (->> data
       (tree-seq #(or (map? %)
                      (vector? %)
                      (seq? %)) seq)
       (filter #(and (map? %)
                     (some-> % :type (isa? :heraldry/motto))
                     (:ribbon-variant %)))
       (map :ribbon-variant)
       set))
