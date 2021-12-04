(ns heraldry.coat-of-arms.field.type.tierced-per-pall
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.chevron :as chevron]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.core :as math]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]))

(def field-type :heraldry.field.type/tierced-per-pall)

(defmethod field-interface/display-name field-type [_] (string "Tierced per pall"))

(defmethod field-interface/part-names field-type [_] ["middle" "side I" "side II"])

(defmethod interface/options field-type [context]
  (let [line-style (-> (line/options (c/++ context :line)
                                     :fimbriation? false)
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil))
        opposite-line-style (-> (line/options (c/++ context :opposite-line)
                                              :fimbriation? false
                                              :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil))
        extra-line-style (-> (line/options (c/++ context :extra-line)
                                           :fimbriation? false
                                           :inherited-options line-style)
                             (options/override-if-exists [:offset :min] 0)
                             (options/override-if-exists [:base-line] nil))
        direction-anchor-point-option {:type :choice
                                       :choices [[(string "Chief [point]") :chief]
                                                 [(string "Base [point]") :base]
                                                 [(string "Dexter [point]") :dexter]
                                                 [(string "Sinister [point]") :sinister]
                                                 [(string "Top-left") :top-left]
                                                 [(string "Top") :top]
                                                 [(string "Top-right") :top-right]
                                                 [(string "Left") :left]
                                                 [(string "Right") :right]
                                                 [(string "Bottom-left") :bottom-left]
                                                 [(string "Bottom") :bottom]
                                                 [(string "Bottom-right") :bottom-right]
                                                 [(string "Angle") :angle]]
                                       :default :top
                                       :ui {:label (string "Point")}}
        current-direction-anchor-point (options/get-value
                                        (interface/get-raw-data (c/++ context :direction-anchor :point))
                                        direction-anchor-point-option)
        anchor-point-option {:type :choice
                             :choices (case current-direction-anchor-point
                                        :bottom [[(string "Bottom-left") :bottom-left]
                                                 [(string "Bottom") :bottom]
                                                 [(string "Bottom-right") :bottom-right]
                                                 [(string "Left") :left]
                                                 [(string "Right") :right]
                                                 [(string "Angle") :angle]]
                                        :top [[(string "Top-left") :top-left]
                                              [(string "Top") :top]
                                              [(string "Top-right") :top-right]
                                              [(string "Left") :left]
                                              [(string "Right") :right]
                                              [(string "Angle") :angle]]
                                        :left [[(string "Top-left") :top-left]
                                               [(string "Left") :left]
                                               [(string "Bottom-left") :bottom-left]
                                               [(string "Top") :top]
                                               [(string "Bottom") :bottom]
                                               [(string "Angle") :angle]]
                                        :right [[(string "Top-right") :top-right]
                                                [(string "Right") :right]
                                                [(string "Bottom-right") :bottom-right]
                                                [(string "Top") :top]
                                                [(string "Bottom") :bottom]
                                                [(string "Angle") :angle]]
                                        :bottom-left [[(string "Bottom-left") :bottom-left]
                                                      [(string "Bottom") :bottom]
                                                      [(string "Bottom-right") :bottom-right]
                                                      [(string "Top-left") :top-left]
                                                      [(string "Left") :left]
                                                      [(string "Angle") :angle]]
                                        :bottom-right [[(string "Bottom-left") :bottom-left]
                                                       [(string "Bottom") :bottom]
                                                       [(string "Bottom-right") :bottom-right]
                                                       [(string "Right") :right]
                                                       [(string "Top-right") :top-right]
                                                       [(string "Angle") :angle]]
                                        :top-left [[(string "Top-left") :top-left]
                                                   [(string "Top") :top]
                                                   [(string "Top-right") :top-right]
                                                   [(string "Left") :left]
                                                   [(string "Bottom-left") :bottom-left]
                                                   [(string "Angle") :angle]]
                                        :top-right [[(string "Top-left") :top-left]
                                                    [(string "Top") :top]
                                                    [(string "Top-right") :top-right]
                                                    [(string "Left") :left]
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
                                        :bottom :bottom-left
                                        :top :top-right
                                        :left :top-left
                                        :right :bottom-right
                                        :bottom-left :left
                                        :bottom-right :bottom
                                        :top-left :top
                                        :top-right :right
                                        :angle :angle
                                        :bottom-left)
                             :ui {:label (string "Point")}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point {:type :choice
                      :choices [[(string "Chief [point]") :chief]
                                [(string "Base [point]") :base]
                                [(string "Fess [point]") :fess]
                                [(string "Dexter [point]") :dexter]
                                [(string "Sinister [point]") :sinister]
                                [(string "Honour [point]") :honour]
                                [(string "Nombril [point]") :nombril]
                                [(string "Top-left") :top-left]
                                [(string "Top") :top]
                                [(string "Top-right") :top-right]
                                [(string "Left") :left]
                                [(string "Right") :right]
                                [(string "Bottom-left") :bottom-left]
                                [(string "Bottom") :bottom]
                                [(string "Bottom-right") :bottom-right]
                                [(string "Angle") :angle]]
                      :default :fess
                      :ui {:label (string "Point")}}
              :alignment {:type :choice
                          :choices position/alignment-choices
                          :default :middle
                          :ui {:label (string "Alignment")
                               :form-type :radio-select}}
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
                                         :max 80
                                         :default 45
                                         :ui {:label (string "Angle")}})

               (not= current-anchor-point
                     :angle) (assoc :alignment {:type :choice
                                                :choices position/alignment-choices
                                                :default :middle
                                                :ui {:label (string "Alignment")
                                                     :form-type :radio-select}}
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
                                                    :step 0.1}}))
     :line line-style
     :opposite-line opposite-line-style
     :extra-line extra-line-style}))

(defmethod field-interface/render-field field-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        extra-line (interface/get-sanitized-data (c/++ context :extra-line))
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
        {direction-origin-point :real-origin
         direction-anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                               environment
                                               origin
                                               direction-anchor
                                               0
                                               -90)
        pall-angle (math/normalize-angle
                    (v/angle-to-point direction-origin-point
                                      direction-anchor-point))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     pall-angle)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        [mirrored-origin mirrored-anchor] [(chevron/mirror-point pall-angle unadjusted-origin-point origin-point)
                                           (chevron/mirror-point pall-angle unadjusted-origin-point anchor-point)]
        origin-point (v/line-intersection origin-point anchor-point
                                          mirrored-origin mirrored-anchor)
        [relative-right relative-left] (chevron/arm-diagonals pall-angle origin-point anchor-point)
        diagonal-left (v/add origin-point relative-left)
        diagonal-right (v/add origin-point relative-right)
        direction-three (v/add origin-point (v/mul (v/add relative-left relative-right) -1))
        intersection-left (v/find-first-intersection-of-ray origin-point diagonal-left environment)
        intersection-right (v/find-first-intersection-of-ray origin-point diagonal-right environment)
        intersection-three (v/find-first-intersection-of-ray origin-point direction-three environment)
        end-left (-> intersection-left
                     (v/sub origin-point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub origin-point)
                      v/abs)
        end (max end-left end-right)
        {line-left :line
         line-left-start :line-start} (line/create line
                                                   origin-point diagonal-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        {line-right :line
         line-right-end :line-end} (line/create opposite-line
                                                origin-point diagonal-right
                                                :flipped? true
                                                :mirrored? true
                                                :real-start 0
                                                :real-end end
                                                :context context
                                                :environment environment)
        {line-three :line
         line-three-start :line-start} (line/create extra-line
                                                    origin-point intersection-three
                                                    :flipped? true
                                                    :mirrored? true
                                                    :context context
                                                    :environment environment)
        {line-three-reversed :line
         line-three-reversed-start :line-start} (line/create extra-line
                                                             origin-point intersection-three
                                                             :reversed? true
                                                             :context context
                                                             :environment environment)
        fork-infinity-points (cond
                               (<= 45 pall-angle 135) [:left :right]
                               (<= 135 pall-angle 225) [:left :left]
                               (<= 225 pall-angle 315) [:top :top]
                               :else [:right :right])
        side-infinity-points (cond
                               (<= 45 pall-angle 135) [:bottom :top]
                               (<= 135 pall-angle 225) [:left :right]
                               (<= 225 pall-angle 315) [:top :bottom]
                               :else [:right :left])
        parts [[["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :counter-clockwise
                                fork-infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/add intersection-three
                            line-three-reversed-start)
                 (path/stitch line-three-reversed)
                 "L" origin-point
                 (path/stitch line-right)
                 (infinity/path :clockwise
                                side-infinity-points
                                [(v/add diagonal-right
                                        line-right-end)
                                 (v/add direction-three
                                        line-three-reversed-start)])
                 "z"]
                [top-left
                 bottom-right]]

               [["M" (v/add diagonal-left
                            line-left-start)
                 (path/stitch line-left)
                 "L" origin-point
                 (path/stitch line-three)
                 (infinity/path :clockwise
                                (reverse side-infinity-points)
                                [(v/add direction-three
                                        line-three-start)
                                 (v/add diagonal-left
                                        line-left-start)])
                 "z"]
                [top-left
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      context parts
      [:all
       [(path/make-path
         ["M" (v/add direction-three
                     line-three-reversed-start)
          (path/stitch line-three-reversed)])]
       nil]
      environment]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add diagonal-left
                                line-left-start)
                     (path/stitch line-left)])}]
        [:path {:d (path/make-path
                    ["M" origin-point
                     (path/stitch line-right)])}]
        [:path {:d (path/make-path
                    ["M" origin-point
                     (path/stitch line-three)])}]])]))
