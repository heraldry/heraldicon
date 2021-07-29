(ns heraldry.collection.element
  (:require [heraldry.interface :as interface]))

(def default-options
  {:name {:type :text
          :ui {:label "Name"}}
   :reference {:ui {:label "Arms"
                    :form-type :arms-reference-select}}
   :pin-reference? {:type :boolean
                    :default false
                    :ui {:label "Pin arms version?"
                         :tooltip "If enabled future changes to the arms won't automatically be reflected here, but can still manually be updated."
                         :form-type :pin-arms-checkbox}}})

(defmethod interface/component-options :heraldry.options/collection-element [_path _data]
  default-options)
