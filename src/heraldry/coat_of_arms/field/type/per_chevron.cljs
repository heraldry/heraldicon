(ns heraldry.coat-of-arms.field.type.per-chevron
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.chevron :as chevron]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def field-type :heraldry.field.type/per-chevron)

(defmethod field-interface/display-name field-type [_] (string "Per chevron"))

(defmethod field-interface/part-names field-type [_] ["chief" "base"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        direction-anchor-point-option {:type :choice
                                       :choices [[(string "Chief [point]") :chief]
                                                 [(string "Base [point]") :base]
                                                 [(string "Dexter [point]") :dexter]
                                                 [(string "Sinister [point]") :sinister]
                                                 [(string "Top-left") :top-left]
                                                 [(string "Top-right") :top-right]
                                                 [(string "Bottom-left") :bottom-left]
                                                 [(string "Bottom-right") :bottom-right]
                                                 [(string "Angle") :angle]]
                                       :default :base
                                       :ui {:label (string "Point")}}
        current-direction-anchor-point (options/get-value
                                        (interface/get-raw-data (c/++ context :direction-anchor :point))
                                        direction-anchor-point-option)
        anchor-point-option {:type :choice
                             :choices (case current-direction-anchor-point
                                        :base [[(string "Bottom-left") :bottom-left]
                                               [(string "Bottom-right") :bottom-right]
                                               [(string "Left") :left]
                                               [(string "Right") :right]
                                               [(string "Angle") :angle]]
                                        :chief [[(string "Top-left") :top-left]
                                                [(string "Top-right") :top-right]
                                                [(string "Left") :left]
                                                [(string "Right") :right]
                                                [(string "Angle") :angle]]
                                        :dexter [[(string "Top-left") :top-left]
                                                 [(string "Bottom-left") :bottom-left]
                                                 [(string "Top") :top]
                                                 [(string "Bottom") :bottom]
                                                 [(string "Angle") :angle]]
                                        :sinister [[(string "Top-right") :top-right]
                                                   [(string "Bottom-right") :bottom-right]
                                                   [(string "Top") :top]
                                                   [(string "Bottom") :bottom]
                                                   [(string "Angle") :angle]]
                                        :bottom-left [[(string "Bottom") :bottom]
                                                      [(string "Bottom-right") :bottom-right]
                                                      [(string "Top-left") :top-left]
                                                      [(string "Left") :left]
                                                      [(string "Angle") :angle]]
                                        :bottom-right [[(string "Bottom-left") :bottom-left]
                                                       [(string "Bottom") :bottom]
                                                       [(string "Right") :right]
                                                       [(string "Top-right") :top-right]
                                                       [(string "Angle") :angle]]
                                        :top-left [[(string "Top") :top]
                                                   [(string "Top-right") :top-right]
                                                   [(string "Left") :left]
                                                   [(string "Bottom-left") :bottom-left]
                                                   [(string "Angle") :angle]]
                                        :top-right [[(string "Top-left") :top-left]
                                                    [(string "Top") :top]
                                                    [(string "Left") :right]
                                                    [(string "Bottom-right") :bottom-right]
                                                    [(string "Angle") :angle]]
                                        [[(string "Top-left") :top-left]
                                         [(string "Top") :top]
                                         [(string "Top-right") :top-right]
                                         [(string "Left") :left]
                                         [(string "Right") :right]
                                         [(string "Bottom-left") :bottom-left]
                                         [(string "Bottom") :bottom]
                                         [(string "Bottom-right") :bottom-right]
                                         [(string "Angle") :angle]])
                             :default (case current-direction-anchor-point
                                        :base :bottom-left
                                        :chief :top-right
                                        :dexter :top-left
                                        :sinister :bottom-right
                                        :bottom-left :left
                                        :bottom-right :right
                                        :top-left :left
                                        :top-right :right
                                        :angle :angle
                                        :bottom-left)
                             :ui {:label (string "Point")}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point {:type :choice
                      :choices [[(string "Fess [point]") :fess]
                                [(string "Chief [point]") :chief]
                                [(string "Base [point]") :base]
                                [(string "Honour [point]") :honour]
                                [(string "Nombril [point]") :nombril]
                                [(string "Top-left") :top-left]
                                [(string "Top") :top]
                                [(string "Top-right") :top-right]
                                [(string "Left") :left]
                                [(string "Right") :right]
                                [(string "Bottom-left") :bottom-left]
                                [(string "Bottom") :bottom]
                                [(string "Bottom-right") :bottom-right]]
                      :default :fess
                      :ui {:label (string "Point")}}
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
     :direction-anchor (cond-> {:point direction-anchor-point-option
                                :ui {:label (string "Issuant")
                                     :form-type :position}}

                         (= current-direction-anchor-point
                            :angle) (assoc :angle {:type :range
                                                   :min -180
                                                   :max 180
                                                   :default 0
                                                   :ui {:label (string "Angle")}})

                         (not= current-direction-anchor-point
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
     :line line-style
     :opposite-line opposite-line-style
     :geometry {:size {:type :range
                       :min 0.1
                       :max 90
                       :default 25
                       :ui {:label (string "Size")
                            :step 0.1}}
                :ui {:label (string "Geometry")
                     :form-type :geometry}}}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        direction-anchor (interface/get-sanitized-data (c/++ context :direction-anchor))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        raw-direction-anchor (interface/get-raw-data (c/++ context :direction-anchor))
        direction-anchor (cond-> direction-anchor
                           (-> direction-anchor
                               :point
                               #{:left
                                 :right
                                 :top
                                 :bottom}) (->
                                            (assoc :offset-x (or (:offset-x raw-direction-anchor)
                                                                 (:offset-x origin)))
                                            (assoc :offset-y (or (:offset-y raw-direction-anchor)
                                                                 (:offset-y origin)))))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               90)
        chevron-angle (math/normalize-angle
                       (v/angle-to-point direction-origin-point
                                         direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     chevron-angle)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point chevron-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point chevron-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-left relative-right] (chevron/arm-diagonals chevron-angle origin-point anchor-point)
        diagonal-left (v/add origin-point relative-left)
        diagonal-right (v/add origin-point relative-right)
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-right environment)
        end-left (-> intersection-left
                     (v/sub origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start
         :as line-left-data} (line/create line
                                          origin-point diagonal-left
                                          :real-start 0
                                          :real-end end
                                          :reversed? true
                                          :context context
                                          :environment environment)
        {line-right :line
         line-right-end :line-end
         :as line-right-data} (line/create opposite-line
                                           origin-point diagonal-right
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        infinity-points (cond
                          (<= 45 chevron-angle 135) [:right :left]
                          (<= 135 chevron-angle 225) [:bottom :top]
                          (<= 225 chevron-angle 315) [:left :right]
                          :else [:top :bottom])
        parts [[["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left top-right
                 bottom-left bottom-right]]

               [["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all nil]
      environment]
     [line/render line [line-left-data
                        line-right-data] diagonal-left outline? context]]))
