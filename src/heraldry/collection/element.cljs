(ns heraldry.collection.element
  (:require
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]))

(defmethod interface/options :heraldry.component/collection-element [_context]
  {:name {:type :text
          :ui {:label strings/name}}
   :reference {:ui {:label strings/arms
                    :form-type :arms-reference-select}}})
