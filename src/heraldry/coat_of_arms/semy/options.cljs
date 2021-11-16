(ns heraldry.coat-of-arms.semy.options
  (:require
   [heraldry.interface :as interface]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defmethod interface/options :heraldry.component/semy [_context]
  (-> {:layout {:num-fields-x {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label strings/num-columns}}
                :num-fields-y {:type :range
                               :min 1
                               :max 20
                               :default 6
                               :integer? true
                               :ui {:label strings/num-rows}}
                :offset-x {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label strings/offset-x
                                :step 0.01}}
                :offset-y {:type :range
                           :min -1
                           :max 1
                           :default 0
                           :ui {:label strings/offset-y
                                :step 0.01}}
                :stretch-x {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label strings/stretch-x
                                 :step 0.01}}
                :stretch-y {:type :range
                            :min 0.5
                            :max 2
                            :default 1
                            :ui {:label strings/stretch-y
                                 :step 0.01}}
                :rotation {:type :range
                           :min -90
                           :max 90
                           :default 0
                           :ui {:label strings/rotation}}
                :ui {:label strings/layout
                     :form-type :semy-layout}}

       :rectangular? {:type :boolean
                      :default false
                      :ui {:label {:en "Rectangular grid"
                                   :de "Rechteckiges Gitter"}}}
       :manual-blazon {:type :text
                       :default nil
                       :ui {:label strings/manual-blazon}}}))
