(ns heraldry.collection.element
  (:require [heraldry.interface :as interface]))

(def default-options
  {:name {:type :text
          :ui {:label "Name"}}
   :reference {:ui {:label "Arms"
                    :form-type :arms-reference-select}}})

(defmethod interface/component-options :heraldry.options/collection-element [_path _data]
  default-options)
