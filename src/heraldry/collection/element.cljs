(ns heraldry.collection.element
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/collection-element [_context]
  #{})

(defmethod interface/options :heraldry.component/collection-element [_context]
  {:name {:type :text
          :ui {:label (string "Name")}}
   :reference {:ui {:label (string "Arms")
                    :form-type :arms-reference-select}}})
