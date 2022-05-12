(ns heraldicon.entity.arms
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/arms-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

(defmethod interface/options :heraldry.component/arms-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
   :tags {:ui {:form-type :tags}}})
