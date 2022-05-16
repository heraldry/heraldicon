(ns heraldicon.heraldry.various
  (:require
   [heraldicon.font :as font]
   [heraldicon.interface :as interface]))

(derive :heraldry/collection-element :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/collection-element [_context]
  #{})

(defmethod interface/options :heraldry/collection-element [_context]
  {:name {:type :text
          :ui {:label :string.option/name}}
   :reference {:ui {:label :string.option/arms
                    :form-type :arms-reference-select}}})

(derive :heraldry/collection :heraldry.options/root)

(defmethod interface/options-subscriptions :heraldry/collection [_context]
  #{})

(defmethod interface/options :heraldry/collection [_context]
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label :string.option/number-of-columns}}
   :font font/default-options})
