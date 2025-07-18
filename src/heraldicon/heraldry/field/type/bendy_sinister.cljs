(ns heraldicon.heraldry.field.type.bendy-sinister
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.options :as options]))

(def field-type :heraldry.field.type/bendy-sinister)

(defmethod field.interface/display-name field-type [_] :string.field.type/bendy-sinister)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation false)
        anchor-point-option {:type :option.type/choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :hoist
                                        :fly
                                        :top-right
                                        :center
                                        :bottom-left])
                             :default :top-right
                             :ui/label :string.option/point}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :option.type/choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-right [:fess
                                                          :chief
                                                          :base
                                                          :honour
                                                          :nombril
                                                          :hoist
                                                          :fly
                                                          :bottom-left
                                                          :center
                                                          :angle]
                                              :bottom-left [:fess
                                                            :chief
                                                            :base
                                                            :honour
                                                            :nombril
                                                            :hoist
                                                            :fly
                                                            :top-right
                                                            :center
                                                            :angle]
                                              [:top-right
                                               :bottom-left
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-right :fess
                                             :bottom-left :fess
                                             :top-right)
                                  :ui/label :string.option/point}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
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
     :orientation (cond-> {:point orientation-point-option
                           :ui/label :string.option/orientation
                           :ui/element :ui.element/position}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :option.type/range
                                              :min 0
                                              :max 360
                                              :default 45
                                              :ui/label :string.option/angle})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :option.type/range
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
                                                    :ui/step 0.1}))
     :layout {:num-fields-y {:type :option.type/range
                             :min 1
                             :max 20
                             :default 6
                             :ui/label :string.option/subfields-y
                             :ui/element :ui.element/field-layout-num-fields-y}
              :num-base-fields {:type :option.type/range
                                :min 2
                                :max 16
                                :default 2
                                :integer? true
                                :ui/label :string.option/base-fields
                                :ui/element :ui.element/field-layout-num-base-fields}
              :offset-y {:type :option.type/range
                         :min -1
                         :max 1
                         :default 0
                         :ui/label :string.option/offset-y
                         :ui/step 0.01}
              :stretch-y {:type :option.type/range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui/label :string.option/stretch-y
                          :ui/step 0.01}
              :ui/label :string.option/layout
              :ui/element :ui.element/field-layout}
     :line line-style}))

(defmethod interface/properties field-type [context]
  ((get-method interface/properties :heraldry.field.type/bendy) context))

(defmethod interface/subfield-environments field-type [context]
  ((get-method interface/subfield-environments :heraldry.field.type/bendy) context))

(defmethod interface/subfield-render-shapes field-type [context]
  ((get-method interface/subfield-render-shapes :heraldry.field.type/barry) context))
