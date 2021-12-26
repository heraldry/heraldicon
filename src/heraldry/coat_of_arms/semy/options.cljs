(ns heraldry.coat-of-arms.semy.options
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/semy [_context]
  #{})

(defmethod interface/options :heraldry.component/semy [_context]
  (-> {:layout {:num-fields-x {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label (string "Number of columns")}}
                :num-fields-y {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label (string "Number of rows")}}
                :offset-x {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label (string "Offset x")
                                :step 0.01}}
                :offset-y {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label (string "Offset y")
                                :step 0.01}}
                :stretch-x {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label (string "Stretch x")
                                 :step 0.01}}
                :stretch-y {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label (string "Stretch y")
                                 :step 0.01}}
                :rotation {:type :range
                           :min -90
                           :max 90
                           :default 0
                           :ui {:label (string "Rotation")}}
                :ui {:label (string "Layout")
                     :form-type :semy-layout}}

       :rectangular? {:type :boolean
                      :default false
                      :ui {:label (string "Rectangular grid")}}
       :manual-blazon {:type :text
                       :default nil
                       :ui {:label (string "Manual blazon")}}}))
