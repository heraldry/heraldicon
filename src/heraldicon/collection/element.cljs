(ns heraldicon.collection.element
  (:require
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/collection-element [_context]
  #{})

(defmethod interface/options :heraldry.component/collection-element [_context]
  {:name {:type :text
          :ui {:label :string.option/name}}
   :reference {:ui {:label :string.option/arms
                    :form-type :arms-reference-select}}})
