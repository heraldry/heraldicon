(ns heraldicon.entity.collection.element
  (:require
   [heraldicon.interface :as interface]))

(derive :heraldicon.entity.collection/element :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldicon.entity.collection/element [_context]
  #{})

(defmethod interface/options :heraldicon.entity.collection/element [_context]
  {:name {:type :text
          :ui {:label :string.option/name}}
   :reference {:ui {:label :string.option/arms
                    :form-type :arms-reference-select}}})