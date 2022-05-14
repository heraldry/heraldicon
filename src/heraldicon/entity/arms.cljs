(ns heraldicon.entity.arms
  (:require
   [heraldicon.config :as config]
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.id :as id]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry/arms-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

(defmethod interface/options :heraldry/arms-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
   :tags {:ui {:form-type :tags}}})

(defn short-url [arms-data]
  (if (= (config/get :stage) "prod")
    (let [{:keys [id version]} arms-data]
      (when (and id version)
        (if (zero? version)
          (str "https://coa.to/" (id/for-url id))
          (str "https://coa.to/" (id/for-url id) "/" version))))
    "https://dev"))
