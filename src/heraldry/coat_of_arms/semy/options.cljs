(ns heraldry.coat-of-arms.semy.options
  (:require
   [heraldry.interface :as interface]))

(defmethod interface/options-subscriptions :heraldry.component/semy [_context]
  #{})

(defmethod interface/options :heraldry.component/semy [_context]
  (-> {:layout {:num-fields-x {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label :string.option/number-of-columns}}
                :num-fields-y {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label :string.option/number-of-rows}}
                :offset-x {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label :string.option/offset-x
                                :step 0.01}}
                :offset-y {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label :string.option/offset-y
                                :step 0.01}}
                :stretch-x {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label :string.option/stretch-x
                                 :step 0.01}}
                :stretch-y {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label :string.option/stretch-y
                                 :step 0.01}}
                :rotation {:type :range
                           :min -90
                           :max 90
                           :default 0
                           :ui {:label :string.option/rotation}}
                :ui {:label :string.option/layout
                     :form-type :semy-layout}}

       :rectangular? {:type :boolean
                      :default false
                      :ui {:label :string.option/rectangular?}}
       :manual-blazon {:type :text
                       :default nil
                       :ui {:label :string.option/manual-blazon}}}))
