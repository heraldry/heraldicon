(ns heraldry.coat-of-arms.charge-group.options
  (:require
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.util :as util]))

(def type-choices
  [[:string.charge-group.type/rows :heraldry.charge-group.type/rows]
   [:string.charge-group.type/columns :heraldry.charge-group.type/columns]
   [:string.charge-group.type/arc :heraldry.charge-group.type/arc]
   [:string.charge-group.type/in-orle :heraldry.charge-group.type/in-orle]])

(def type-map
  (util/choices->map type-choices))

(def type-option
  {:type :choice
   :choices type-choices
   :ui {:label :string.option/type
        :form-type :charge-group-type-select}})

(def shared-options
  {:origin {:point {:type :choice
                    :choices position/point-choices
                    :default :fess
                    :ui {:label :string.option/point}}
            :offset-x {:type :range
                       :min -45
                       :max 45
                       :default 0
                       :ui {:label :string.option/offset-x
                            :step 0.1}}
            :offset-y {:type :range
                       :min -45
                       :max 45
                       :default 0
                       :ui {:label :string.option/offset-y
                            :step 0.1}}
            :ui {:label :string.option/origin
                 :form-type :position}}
   :manual-blazon {:type :text
                   :default nil
                   :ui {:label :string.option/manual-blazon}}})

(defn rows-or-columns [_context]
  (-> shared-options
      (merge {:spacing {:type :range
                        :min 1
                        :max 100
                        :default 40
                        :ui {:label :string.option/spacing
                             :step 0.1}}
              :stretch {:type :range
                        :min 0
                        :max 5
                        :default 1
                        :ui {:label :string.option/stretch
                             :step 0.01}}
              :strip-angle {:type :range
                            :min -90
                            :max 90
                            :default 0
                            :ui {:label :string.option/strip-angle
                                 :step 1}}})))

(defmethod interface/options :heraldry.charge-group.type/rows [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/columns [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/arc [_context]
  (-> shared-options
      (merge {:start-angle {:type :range
                            :min -180
                            :max 180
                            :default 0
                            :ui {:label :string.option/start-angle
                                 :step 1}}
              :arc-angle {:type :range
                          :min 0
                          :max 360
                          :default 360
                          :ui {:label :string.option/arc-angle
                               :step 1}}
              :slots {:type :range
                      :min 1
                      :max 20
                      :default 5
                      :integer? true
                      :ui {:label :string.option/number
                           :form-type :charge-group-slot-number}}
              :radius {:type :range
                       :min 0
                       :max 100
                       :default 30
                       :ui {:label :string.option/radius
                            :step 0.1}}
              :arc-stretch {:type :range
                            :min 0
                            :max 5
                            :default 1
                            :ui {:label :string.option/stretch
                                 :step 0.01}}
              :rotate-charges? {:type :boolean
                                :default false
                                :ui {:label :string.option/rotate-charges?}}})))

(defmethod interface/options :heraldry.charge-group.type/in-orle [_context]
  (-> shared-options
      (dissoc :origin)
      (merge {:distance {:type :range
                         :min 0
                         :max 30
                         :default 10
                         :ui {:label :string.option/distance
                              :step 0.1}}
              :offset {:type :range
                       :min -1
                       :max 1
                       :default 0
                       :ui {:label :string.option/offset
                            :step 0.01}}
              :slots {:type :range
                      :min 1
                      :max 30
                      :default 5
                      :integer? true
                      :ui {:label :string.option/number
                           :form-type :charge-group-slot-number}}
              ;; TODO: this should be added at some point, but there are some issues
              ;; around corners, so I'll leave it for now
              #_#_:rotate-charges? {:type :boolean
                                    :default false
                                    :ui {:label :string.option/rotate-charges?}}})))

(defmethod interface/options-subscriptions :heraldry.component/charge-group [_context]
  #{[:type]})

(defmethod interface/options :heraldry.component/charge-group [context]
  (-> context
      (assoc :dispatch-value (interface/get-raw-data (c/++ context :type)))
      interface/options
      (assoc :type type-option)))

(defmethod interface/options-subscriptions :heraldry.component/charge-group-strip [_context]
  #{[:type]})

(defmethod interface/options :heraldry.component/charge-group-strip [_context]
  {:slots {:type :range
           :min 0
           :max 10
           :default 3
           :integer? true
           :ui {:label :string.option/number
                :form-type :charge-group-slot-number}}
   :stretch {:type :range
             :min 0
             :max 5
             :default 1
             :ui {:label :string.option/stretch
                  :step 0.01}}
   :offset {:type :range
            :min -3
            :max 3
            :default 0
            :ui {:label :string.option/offset
                 :step 0.01}}})
