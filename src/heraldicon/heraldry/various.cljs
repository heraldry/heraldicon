(ns heraldicon.heraldry.various
  (:require
   [heraldicon.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/helm [_context]
  #{})

(defmethod interface/options :heraldry.component/helm [_context]
  {})

(defmethod interface/options-subscriptions :heraldry.component/helms [_context]
  #{})

(defmethod interface/options :heraldry.component/helms [_context]
  {})

(defmethod interface/options-subscriptions :heraldry.component/ornaments [_context]
  #{})

(defmethod interface/options :heraldry.component/ornaments [_context]
  {})

(defmethod interface/options-subscriptions :heraldry.component/collection-element [_context]
  #{})

(defmethod interface/options :heraldry.component/collection-element [_context]
  {:name {:type :text
          :ui {:label :string.option/name}}
   :reference {:ui {:label :string.option/arms
                    :form-type :arms-reference-select}}})

(defmethod interface/options-subscriptions :heraldry.component/collection [_context]
  #{})

(defmethod interface/options :heraldry.component/collection [_context]
  {:num-columns {:type :range
                 :default 6
                 :min 1
                 :max 10
                 :ui {:label :string.option/number-of-columns}}})
