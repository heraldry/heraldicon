(ns heraldicon.entity.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.interface :as interface]))

(derive :heraldicon/ribbon :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldicon/ribbon [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/options :heraldicon/ribbon [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :tags {:ui {:form-type :tags}}})
