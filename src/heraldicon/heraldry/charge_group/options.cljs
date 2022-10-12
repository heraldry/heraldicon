(ns heraldicon.heraldry.charge-group.options
  (:require
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(def ^:private type-choices
  [[:string.charge-group.type/rows :heraldry.charge-group.type/rows]
   [:string.charge-group.type/columns :heraldry.charge-group.type/columns]
   [:string.charge-group.type/arc :heraldry.charge-group.type/arc]
   [:string.charge-group.type/in-orle :heraldry.charge-group.type/in-orle]])

(def type-map
  (options/choices->map type-choices))

(def ^:private shared-options
  {:adapt-to-ordinaries? {:type :option.type/boolean
                          :default true
                          :ui/label :string.option/adapt-to-ordinaries?}
   :type {:type :option.type/choice
          :choices type-choices
          :ui/label :string.option/type
          :ui/element :ui.element/charge-group-type-select}
   :anchor {:point {:type :option.type/choice
                    :choices (position/anchor-choices
                              [:fess
                               :chief
                               :base
                               :dexter
                               :sinister
                               :honour
                               :nombril
                               :hoist
                               :fly
                               :top-left
                               :top
                               :top-right
                               :left
                               :center
                               :right
                               :bottom-left
                               :bottom
                               :bottom-right])
                    :default :fess
                    :ui/label :string.option/point}
            :offset-x {:type :option.type/range
                       :min -45
                       :max 45
                       :default 0
                       :ui/label :string.option/offset-x
                       :ui/step 0.1}
            :offset-y {:type :option.type/range
                       :min -45
                       :max 45
                       :default 0
                       :ui/label :string.option/offset-y
                       :ui/step 0.1}
            :ui/label :string.option/anchor
            :ui/element :ui.element/position}
   :manual-blazon options/manual-blazon})

(defn- rows-or-columns [_context]
  (merge shared-options
         {:spacing {:type :option.type/range
                    :min 1
                    :max 100
                    :default 40
                    :ui/label :string.option/spacing
                    :ui/step 0.1}
          :stretch {:type :option.type/range
                    :min 0
                    :max 5
                    :default 1
                    :ui/label :string.option/stretch
                    :ui/step 0.01}
          :strip-angle {:type :option.type/range
                        :min -90
                        :max 90
                        :default 0
                        :ui/label :string.option/strip-angle
                        :ui/step 1}}))

(defmethod interface/options :heraldry.charge-group.type/rows [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/columns [context]
  (rows-or-columns context))

(defmethod interface/options :heraldry.charge-group.type/arc [_context]
  (merge shared-options
         {:start-angle {:type :option.type/range
                        :min -180
                        :max 180
                        :default 0
                        :ui/label :string.option/start-angle
                        :ui/step 1}
          :arc-angle {:type :option.type/range
                      :min 0
                      :max 360
                      :default 360
                      :ui/label :string.option/arc-angle
                      :ui/step 1}
          :slots {:type :option.type/range
                  :min 1
                  :max 20
                  :default 5
                  :integer? true
                  :ui/label :string.option/number
                  :ui/element :ui.element/charge-group-slot-number}
          :radius {:type :option.type/range
                   :min 0
                   :max 100
                   :default 30
                   :ui/label :string.option/radius
                   :ui/step 0.1}
          :arc-stretch {:type :option.type/range
                        :min 0
                        :max 5
                        :default 1
                        :ui/label :string.option/stretch
                        :ui/step 0.01}
          :rotate-charges? {:type :option.type/boolean
                            :default false
                            :ui/label :string.option/rotate-charges?}}))

(defmethod interface/options :heraldry.charge-group.type/in-orle [_context]
  (-> shared-options
      (dissoc :anchor)
      (merge {:distance {:type :option.type/range
                         :min 0
                         :max 30
                         :default 6
                         :ui/label :string.option/distance
                         :ui/step 0.1}
              :offset {:type :option.type/range
                       :min -1
                       :max 1
                       :default 0
                       :ui/label :string.option/offset
                       :ui/step 0.01}
              :slots {:type :option.type/range
                      :min 1
                      :max 30
                      :default 5
                      :integer? true
                      :ui/label :string.option/number
                      :ui/element :ui.element/charge-group-slot-number}
              ;; TODO: this should be added at some point, but there are some issues
              ;; around corners, so I'll leave it for now
              #_#_:rotate-charges? {:type :option.type/boolean
                                    :default false
                                    :ui/label :string.option/rotate-charges?}})))

(derive :heraldry/charge-group :heraldry.options/root)

(derive :heraldry.charge-group.element.type/strip :heraldry.options/root)

(defmethod interface/options :heraldry.charge-group.element.type/strip [_context]
  {:slots {:type :option.type/range
           :min 0
           :max 10
           :default 3
           :integer? true
           :ui/label :string.option/number
           :ui/element :ui.element/charge-group-slot-number}
   :stretch {:type :option.type/range
             :min 0
             :max 5
             :default 1
             :ui/label :string.option/stretch
             :ui/step 0.01}
   :offset {:type :option.type/range
            :min -3
            :max 3
            :default 0
            :ui/label :string.option/offset
            :ui/step 0.01}})
