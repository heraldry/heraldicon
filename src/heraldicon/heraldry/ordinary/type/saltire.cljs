(ns heraldicon.heraldry.ordinary.type.saltire
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.heraldry.shared.saltire :as saltire]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/saltire)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/saltire)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        orientation-point-option {:type :choice
                                  :choices [[:string.option.point-choice/top-left :top-left]
                                            [:string.option.point-choice/top-right :top-right]
                                            [:string.option.point-choice/bottom-left :bottom-left]
                                            [:string.option.point-choice/bottom-right :bottom-right]
                                            [:string.option.orientation-point-choice/angle :angle]]
                                  :default :top-left
                                  :ui {:label :string.option/point}}
        current-orientation-point (options/get-value
                                   (interface/get-raw-data (c/++ context :orientation :point))
                                   orientation-point-option)]
    ;; TODO: perhaps there should be anchor options for the corners?
    ;; so one can align fro top-left to bottom-right
    (-> {:anchor {:point {:type :choice
                          :choices [[:string.option.point-choice/chief :chief]
                                    [:string.option.point-choice/base :base]
                                    [:string.option.point-choice/fess :fess]
                                    [:string.option.point-choice/dexter :dexter]
                                    [:string.option.point-choice/sinister :sinister]
                                    [:string.option.point-choice/honour :honour]
                                    [:string.option.point-choice/nombril :nombril]]
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
                  :ui {:label :string.option/anchor
                       :form-type :position}}
         :orientation (cond-> {:point orientation-point-option
                               :ui {:label :string.option/orientation
                                    :form-type :position}}

                        (= current-orientation-point
                           :angle) (assoc :angle {:type :range
                                                  :min 10
                                                  :max 80
                                                  :default 45
                                                  :ui {:label :string.option/angle}})

                        (not= current-orientation-point
                              :angle) (assoc :alignment {:type :choice
                                                         :choices position/alignment-choices
                                                         :default :middle
                                                         :ui {:label :string.option/alignment
                                                              :form-type :radio-select}}
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
                                                             :step 0.1}}))
         :line line-style
         :geometry {:size {:type :range
                           :min 0.1
                           :max 90
                           :default 25
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary.shared/add-humetty-and-voided context))))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        orientation (interface/get-sanitized-data (c/++ context :orientation))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        unadjusted-anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x unadjusted-anchor-point))
        bottom (assoc (:bottom points) :x (:x unadjusted-anchor-point))
        left (assoc (:left points) :y (:y unadjusted-anchor-point))
        right (assoc (:right points) :y (:y unadjusted-anchor-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((math/percent-of width)))
        {anchor-point :real-anchor
         orientation-point :real-orientation} (position/calculate-anchor-and-orientation
                                               environment
                                               anchor
                                               orientation
                                               band-width
                                               nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals anchor-point orientation-point)
        diagonal-top-left (v/add anchor-point relative-top-left)
        diagonal-top-right (v/add anchor-point relative-top-right)
        diagonal-bottom-left (v/add anchor-point relative-bottom-left)
        diagonal-bottom-right (v/add anchor-point relative-bottom-right)
        angle-bottom-right (v/angle-to-point anchor-point diagonal-bottom-right)
        angle (-> angle-bottom-right (* Math/PI) (/ 180))
        dx (/ band-width 2 (Math/sin angle))
        dy (/ band-width 2 (Math/cos angle))
        offset-top (v/Vector. 0 (- dy))
        offset-bottom (v/Vector. 0 dy)
        offset-left (v/Vector. (- dx) 0)
        offset-right (v/Vector. dx 0)
        corner-top (v/add anchor-point offset-top)
        corner-bottom (v/add anchor-point offset-bottom)
        corner-left (v/add anchor-point offset-left)
        corner-right (v/add anchor-point offset-right)
        top-left-upper (v/add diagonal-top-left offset-top)
        top-left-lower (v/add diagonal-top-left offset-bottom)
        top-right-upper (v/add diagonal-top-right offset-top)
        top-right-lower (v/add diagonal-top-right offset-bottom)
        bottom-left-upper (v/add diagonal-bottom-left offset-top)
        bottom-left-lower (v/add diagonal-bottom-left offset-bottom)
        bottom-right-upper (v/add diagonal-bottom-right offset-top)
        bottom-right-lower (v/add diagonal-bottom-right offset-bottom)
        intersection-top-left-upper (v/find-first-intersection-of-ray corner-top top-left-upper environment)
        intersection-top-right-upper (v/find-first-intersection-of-ray corner-top top-right-upper environment)
        intersection-top-left-lower (v/find-first-intersection-of-ray corner-left top-left-lower environment)
        intersection-top-right-lower (v/find-first-intersection-of-ray corner-right top-right-lower environment)
        intersection-bottom-left-upper (v/find-first-intersection-of-ray corner-left bottom-left-upper environment)
        intersection-bottom-right-upper (v/find-first-intersection-of-ray corner-right bottom-right-upper environment)
        intersection-bottom-left-lower (v/find-first-intersection-of-ray corner-bottom bottom-left-lower environment)
        intersection-bottom-right-lower (v/find-first-intersection-of-ray corner-bottom bottom-right-lower environment)
        end-top-left-upper (-> intersection-top-left-upper
                               (v/sub corner-top)
                               v/abs)
        end-top-right-upper (-> intersection-top-right-upper
                                (v/sub corner-top)
                                v/abs)
        end-top-left-lower (-> intersection-top-left-lower
                               (v/sub corner-left)
                               v/abs)
        end-top-right-lower (-> intersection-top-right-lower
                                (v/sub corner-right)
                                v/abs)
        end-bottom-left-upper (-> intersection-bottom-left-upper
                                  (v/sub corner-left)
                                  v/abs)
        end-bottom-right-upper (-> intersection-bottom-right-upper
                                   (v/sub corner-right)
                                   v/abs)
        end-bottom-left-lower (-> intersection-bottom-left-lower
                                  (v/sub corner-bottom)
                                  v/abs)
        end-bottom-right-lower (-> intersection-bottom-right-lower
                                   (v/sub corner-bottom)
                                   v/abs)
        end (max end-top-left-upper
                 end-top-right-upper
                 end-top-left-lower
                 end-top-right-lower
                 end-bottom-left-upper
                 end-bottom-right-upper
                 end-bottom-left-lower
                 end-bottom-right-lower)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (math/percent-of height))
                 (update-in [:fimbriation :thickness-2] (math/percent-of height)))
        {line-top-left-lower :line
         line-top-left-lower-start :line-start
         line-top-left-lower-min :line-min
         :as line-top-left-lower-data} (line/create line
                                                    corner-left top-left-lower
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-top-left-upper :line
         line-top-left-upper-start :line-start
         :as line-top-left-upper-data} (line/create line
                                                    corner-top top-left-upper
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-top-right-upper :line
         line-top-right-upper-start :line-start
         :as line-top-right-upper-data} (line/create line
                                                     corner-top top-right-upper
                                                     :real-start 0
                                                     :real-end end
                                                     :context context
                                                     :environment environment)
        {line-top-right-lower :line
         line-top-right-lower-start :line-start
         :as line-top-right-lower-data} (line/create line
                                                     corner-right top-right-lower
                                                     :reversed? true
                                                     :real-start 0
                                                     :real-end end
                                                     :context context
                                                     :environment environment)
        {line-bottom-right-upper :line
         line-bottom-right-upper-start :line-start
         :as line-bottom-right-upper-data} (line/create line
                                                        corner-right bottom-right-upper
                                                        :real-start 0
                                                        :real-end end
                                                        :context context
                                                        :environment environment)
        {line-bottom-right-lower :line
         line-bottom-right-lower-start :line-start
         :as line-bottom-right-lower-data} (line/create line
                                                        corner-bottom bottom-right-lower
                                                        :reversed? true
                                                        :real-start 0
                                                        :real-end end
                                                        :context context
                                                        :environment environment)
        {line-bottom-left-lower :line
         line-bottom-left-lower-start :line-start
         :as line-bottom-left-lower-data} (line/create line
                                                       corner-bottom bottom-left-lower
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        {line-bottom-left-upper :line
         line-bottom-left-upper-start :line-start
         :as line-bottom-left-upper-data} (line/create line
                                                       corner-left bottom-left-upper
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add corner-left
                           line-top-left-lower-start)
                (path/stitch line-top-left-lower)
                "L" (v/add top-left-upper
                           line-top-left-upper-start)
                (path/stitch line-top-left-upper)
                "L" (v/add corner-top
                           line-top-right-upper-start)
                (path/stitch line-top-right-upper)
                "L" (v/add top-right-lower
                           line-top-right-lower-start)
                (path/stitch line-top-right-lower)
                "L" (v/add corner-right
                           line-bottom-right-upper-start)
                (path/stitch line-bottom-right-upper)
                "L" (v/add bottom-right-lower
                           line-bottom-right-lower-start)
                (path/stitch line-bottom-right-lower)
                "L" (v/add corner-bottom
                           line-bottom-left-lower-start)
                (path/stitch line-bottom-left-lower)
                "L" (v/add bottom-left-upper
                           line-bottom-left-upper-start)
                (path/stitch line-bottom-left-upper)
                "z"]
               width
               band-width
               context)
        part [shape
              [top bottom left right]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [:<>
       [line/render line [line-top-left-upper-data
                          line-top-right-upper-data] top-left-upper outline? context]
       [line/render line [line-top-right-lower-data
                          line-bottom-right-upper-data] top-right-lower outline? context]
       [line/render line [line-bottom-right-lower-data
                          line-bottom-left-lower-data] bottom-right-lower outline? context]
       [line/render line [line-bottom-left-upper-data
                          line-top-left-lower-data] bottom-left-upper outline? context]])
     [:<>
      (for [[chevron-angle
             corner-point
             half-joint-angle] [[270 corner-top (- 90 angle-bottom-right)]
                                [180 corner-left angle-bottom-right]
                                [0 corner-right angle-bottom-right]
                                [90 corner-bottom (- 90 angle-bottom-right)]]]
        ^{:key chevron-angle}
        [cottising/render-chevron-cottise
         (c/++ context :cottising :cottise-1)
         :cottise-2 :cottise-opposite-1
         :distance-fn (fn [distance half-joint-angle-rad]
                        (-> (+ distance)
                            (/ 100)
                            (* width)
                            (- line-top-left-lower-min)
                            (/ (if (zero? half-joint-angle-rad)
                                 0.00001
                                 (Math/sin half-joint-angle-rad)))))
         :alignment :right
         :width width
         :height height
         :chevron-angle chevron-angle
         :joint-angle (* 2 half-joint-angle)
         :corner-point corner-point
         :swap-lines? true])]]))
