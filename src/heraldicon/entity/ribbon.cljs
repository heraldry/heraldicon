(ns heraldicon.entity.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry/ribbon-general [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

;; TODO: might not be the right place for it, others live in the coat-of-charge.[thing].options namespaces
(defmethod interface/options :heraldry/ribbon-general [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :is-public {:type :boolean
               :ui {:label :string.option/is-public}}
   :attribution (attribution/options (c/++ context :attribution))
   :ribbon (ribbon/options context)
   :tags {:ui {:form-type :tags}}})
