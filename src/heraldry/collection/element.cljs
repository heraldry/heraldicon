(ns heraldry.collection.element
  (:require [heraldry.interface :as interface]
            [heraldry.strings :as strings]))

(def default-options
  {:name {:type :text
          :ui {:label "Name"}}
   :reference {:ui {:label strings/arms
                    :form-type :arms-reference-select}}})

(defmethod interface/component-options :heraldry.component/collection-element [_path _data]
  default-options)
