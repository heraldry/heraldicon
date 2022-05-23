(ns heraldicon.entity
  (:require
   [heraldicon.context :as c]
   [heraldicon.entity.attribution :as attribution]
   [heraldicon.entity.metadata :as metadata]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(derive :heraldicon/entity :heraldry.options/root)
(derive :heraldicon.entity.type/arms :heraldicon.entity/type)
(derive :heraldicon.entity.type/charge :heraldicon.entity/type)
(derive :heraldicon.entity.type/collection :heraldicon.entity/type)
(derive :heraldicon.entity.type/ribbon :heraldicon.entity/type)
(derive :heraldicon.entity/type :heraldicon/entity)

(defmethod interface/options-subscriptions :heraldicon/entity [_context]
  #{[:attribution :license]
    [:attribution :nature]
    [:attribution :source-license]})

(def access-choices
  [["private" :private]
   ["public" :public]])

(def access-map
  (options/choices->map access-choices))

(defmethod interface/options :heraldicon/entity [context]
  {:name {:type :text
          :default ""
          :ui {:label :string.option/name}}
   :access {:type :choice
            :choices access-choices
            :default :private
            :ui {:label :string.option/is-public
                 :form-type :access}}
   :attribution (attribution/options (c/++ context :attribution))
   :metadata (metadata/options (c/++ context :metadata))
   :tags {:ui {:form-type :tags}}})
