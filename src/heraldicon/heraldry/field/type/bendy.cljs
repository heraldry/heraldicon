(ns heraldicon.heraldry.field.type.bendy
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.field.shared :as shared]
   [heraldicon.heraldry.field.type.barry :as barry]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(def field-type :heraldry.field.type/bendy)

(defmethod field.interface/display-name field-type [_] :string.field.type/bendy)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation? false)
        anchor-point-option {:type :choice
                             :choices (position/anchor-choices
                                       [:fess
                                        :chief
                                        :base
                                        :honour
                                        :nombril
                                        :top-left
                                        :center
                                        :bottom-right])
                             :default :top-left
                             :ui {:label :string.option/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        orientation-point-option {:type :choice
                                  :choices (position/orientation-choices
                                            (case current-anchor-point
                                              :top-left [:fess
                                                         :chief
                                                         :base
                                                         :honour
                                                         :nombril
                                                         :bottom-right
                                                         :center
                                                         :angle]
                                              :bottom-right [:fess
                                                             :chief
                                                             :base
                                                             :honour
                                                             :nombril
                                                             :top-left
                                                             :center
                                                             :angle]
                                              [:top-left
                                               :bottom-right
                                               :angle]))
                                  :default (case current-anchor-point
                                             :top-left :fess
                                             :bottom-right :fess
                                             :top-left)
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    {:anchor {:point anchor-point-option
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
              :ui {:label :string.option/anchor
                   :form-type :position}}
     :orientation (cond-> {:point orientation-point-option
                           :ui {:label :string.option/orientation
                                :form-type :position}}

                    (= current-orientation-point
                       :angle) (assoc :angle {:type :range
                                              :min 0
                                              :max 360
                                              :default 45
                                              :ui {:label :string.option/angle}})

                    (not= current-orientation-point
                          :angle) (assoc :offset-x {:type :range
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
                                                         :step 0.1}}))
     :layout {:num-fields-y {:type :range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui {:label :string.option/subfields-y
                                  :form-type :field-layout-num-fields-y}}
              :num-base-fields {:type :range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui {:label :string.option/base-fields
                                     :form-type :field-layout-num-base-fields}}
              :offset-y {:type :range
                         :min -1
                         :max 1
                         :default 0
                         :ui {:label :string.option/offset-y
                              :step 0.01}}
              :stretch-y {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label :string.option/stretch-y
                               :step 0.01}}
              :ui {:label :string.option/layout
                   :form-type :field-layout}}
     :line line-style}))

(defmethod field.interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        top (:top points)
        bottom (:bottom points)
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               0
                                               nil)
        center-point (v/line-intersection anchor-point orientation-point
                                          top bottom)
        direction (v/sub orientation-point anchor-point)
        direction (v/Vector. (-> direction :x Math/abs)
                             (-> direction :y Math/abs))
        direction-orthogonal (v/orthogonal direction)
        angle (v/angle-to-point v/zero direction)
        required-half-width (v/distance-point-to-line top-left center-point (v/add center-point direction-orthogonal))
        required-half-height (v/distance-point-to-line top-right center-point (v/add center-point direction))
        [parts overlap outlines] (barry/barry-parts
                                  (v/Vector. (- required-half-width) (- required-half-height))
                                  (v/Vector. required-half-width required-half-height)
                                  line outline? context)]
    [:g {:transform (str "translate(" (v/->str center-point) ")"
                         "rotate(" angle ")")}
     [shared/make-subfields
      context parts
      overlap
      environment]
     outlines]))
