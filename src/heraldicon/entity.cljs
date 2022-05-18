(ns heraldicon.entity
  (:require [heraldicon.context :as c]
            [heraldicon.entity.attribution :as attribution]
            [heraldicon.entity.metadata :as metadata]
            [heraldicon.interface :as interface]))

(derive :heraldicon/entity :heraldry.options/root)
(derive :heraldicon/arms :heraldicon/entity)
(derive :heraldicon/charge :heraldicon/entity)
(derive :heraldicon/collection :heraldicon/entity)
(derive :heraldicon/ribbon :heraldicon/entity)

(defmethod interface/options-subscriptions :heraldicon/entity [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

(defmethod interface/options :heraldicon/entity [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
   :tags {:ui {:form-type :tags}}})
