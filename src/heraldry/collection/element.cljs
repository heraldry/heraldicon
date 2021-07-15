(ns heraldry.collection.element
  (:require [heraldry.frontend.ui.interface :as interface]))

(def default-options
  {:name {:type :text
          :ui {:label "Name"}}
   :reference {:ui {:label "Arms"
                    :form-type :arms-reference-select}}})

(defmethod interface/component-options :collection-element [_data _path]
  default-options)
