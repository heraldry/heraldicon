(ns heraldry.coat-of-arms.field.type.bendy-sinister
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.field.type.barry :as barry]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def field-type :heraldry.field.type/bendy-sinister)

(defmethod field-interface/display-name field-type [_] (string "Bendy sinister"))

(defmethod field-interface/part-names field-type [_] nil)

(defmethod interface/options field-type [context]
  (let [line-style (line/options (c/++ context :line)
                                 :fimbriation false)
        origin-point-option {:type :choice
                             :choices [[(string "Fess") :fess]
                                       [(string "Chief") :chief]
                                       [(string "Base") :base]
                                       [(string "Honour") :honour]
                                       [(string "Nombril") :nombril]
                                       [(string "Top-right") :top-right]
                                       [(string "Bottom-left") :bottom-left]]
                             :default :top-right
                             :ui {:label (string "Point")}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        anchor-point-option {:type :choice
                             :choices (case current-origin-point
                                        :top-right [[(string "Fess") :fess]
                                                    [(string "Chief") :chief]
                                                    [(string "Base") :base]
                                                    [(string "Honour") :honour]
                                                    [(string "Nombril") :nombril]
                                                    [(string "Bottom-left") :bottom-left]
                                                    [(string "Angle") :angle]]
                                        :bottom-left [[(string "Fess") :fess]
                                                      [(string "Chief") :chief]
                                                      [(string "Base") :base]
                                                      [(string "Honour") :honour]
                                                      [(string "Nombril") :nombril]
                                                      [(string "Top-right") :top-right]
                                                      [(string "Angle") :angle]]
                                        [[(string "Top-right") :top-right]
                                         [(string "Bottom-left") :bottom-left]
                                         [(string "Angle") :angle]])
                             :default (case current-origin-point
                                        :top-right :fess
                                        :bottom-left :fess
                                        :top-right)
                             :ui {:label (string "Point")}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point origin-point-option
              :offset-x {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label (string "Offset x")
                              :step 0.1}}
              :offset-y {:type :range
                         :min -45
                         :max 45
                         :default 0
                         :ui {:label (string "Offset y")
                              :step 0.1}}
              :ui {:label (string "Origin")
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label (string "Anchor")
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min 0
                                         :max 360
                                         :default 45
                                         :ui {:label (string "Angle")}})

               (not= current-anchor-point
                     :angle) (assoc :offset-x {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label (string "Offset x")
                                                    :step 0.1}}
                                    :offset-y {:type :range
                                               :min -45
                                               :max 45
                                               :default 0
                                               :ui {:label (string "Offset y")
                                                    :step 0.1}}))
     :layout {:num-fields-y {:type :range
                             :min 1
                             :max 20
                             :default 6
                             :integer? true
                             :ui {:label (string "Subfields y")
                                  :form-type :field-layout-num-fields-y}}
              :num-base-fields {:type :range
                                :min 2
                                :max 8
                                :default 2
                                :integer? true
                                :ui {:label (string "Base fields")
                                     :form-type :field-layout-num-base-fields}}
              :offset-y {:type :range
                         :min -1
                         :max 1
                         :default 0
                         :ui {:label (string "Offset y")
                              :step 0.01}}
              :stretch-y {:type :range
                          :min 0.5
                          :max 2
                          :default 1
                          :ui {:label (string "Stretch y")
                               :step 0.01}}
              :ui {:label (string "Layout")
                   :form-type :field-layout}}
     :line line-style}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        top (:top points)
        bottom (:bottom points)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     (update anchor :angle #(when %
                                                              (- %)))
                                     0
                                     nil)
        center-point (v/line-intersection origin-point anchor-point
                                          top bottom)
        direction (v/sub anchor-point origin-point)
        direction (v/v (-> direction :x Math/abs)
                       (-> direction :y Math/abs -))
        direction-orthogonal (v/orthogonal direction)
        angle (v/angle-to-point (v/v 0 0) direction)
        required-half-width (v/distance-point-to-line top-right center-point (v/add center-point direction-orthogonal))
        required-half-height (v/distance-point-to-line top-left center-point (v/add center-point direction))
        [parts overlap outlines] (barry/barry-parts
                                  (v/v (- required-half-width) (- required-half-height))
                                  (v/v required-half-width required-half-height)
                                  line outline? context)]
    [:g {:transform (str "translate(" (v/->str center-point) ")"
                         "rotate(" angle ")")}
     [shared/make-subfields
      context
      parts
      overlap
      environment]
     outlines]))
