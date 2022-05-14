(ns heraldicon.heraldry.various
  (:require
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry/helm [_context]
  #{})

(defmethod interface/options :heraldry/helm [_context]
  {})

(defmethod interface/options-subscriptions :heraldry/helms [_context]
  #{})

(defmethod interface/options :heraldry/helms [_context]
  {})

(defmethod interface/options-subscriptions :heraldry/ornaments [_context]
  #{})

(defmethod interface/options :heraldry/ornaments [_context]
  {})

(defmethod interface/options-subscriptions :heraldry/collection-element [_context]
  #{})

(defmethod interface/options :heraldry/collection-element [_context]
  {:name {:type :text
          :ui {:label :string.option/name}}
   :reference {:ui {:label :string.option/arms
                    :form-type :arms-reference-select}}})

(defmethod interface/options-subscriptions :heraldry/collection [_context]
  #{})

(defmethod interface/options :heraldry/collection [_context]
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label :string.option/number-of-columns}}})
