(ns heraldry.coat-of-arms.ordinary.type.saltire
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary-shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.saltire :as saltire]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/saltire)

(defmethod ordinary-interface/display-name ordinary-type [_] (string "Saltire"))

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        anchor-point-option {:type :choice
                             :choices [[strings/top-left :top-left]
                                       [strings/top-right :top-right]
                                       [strings/bottom-left :bottom-left]
                                       [strings/bottom-right :bottom-right]
                                       [strings/angle :angle]]
                             :default :top-left
                             :ui {:label strings/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    ;; TODO: perhaps there should be origin options for the corners?
    ;; so one can align fro top-left to bottom-right
    (-> {:origin {:point {:type :choice
                          :choices [[strings/chief-point :chief]
                                    [strings/base-point :base]
                                    [strings/fess-point :fess]
                                    [strings/dexter-point :dexter]
                                    [strings/sinister-point :sinister]
                                    [strings/honour-point :honour]
                                    [strings/nombril-point :nombril]]
                          :default :fess
                          :ui {:label strings/point}}
                  :offset-x {:type :range
                             :min -45
                             :max 45
                             :default 0
                             :ui {:label strings/offset-x
                                  :step 0.1}}
                  :offset-y {:type :range
                             :min -45
                             :max 45
                             :default 0
                             :ui {:label strings/offset-y
                                  :step 0.1}}
                  :ui {:label strings/origin
                       :form-type :position}}
         :anchor (cond-> {:point anchor-point-option
                          :ui {:label strings/anchor
                               :form-type :position}}

                   (= current-anchor-point
                      :angle) (assoc :angle {:type :range
                                             :min 10
                                             :max 80
                                             :default 45
                                             :ui {:label strings/angle}})

                   (not= current-anchor-point
                         :angle) (assoc :alignment {:type :choice
                                                    :choices position/alignment-choices
                                                    :default :middle
                                                    :ui {:label strings/alignment
                                                         :form-type :radio-select}}
                                        :offset-x {:type :range
                                                   :min -45
                                                   :max 45
                                                   :default 0
                                                   :ui {:label strings/offset-x
                                                        :step 0.1}}
                                        :offset-y {:type :range
                                                   :min -45
                                                   :max 45
                                                   :default 0
                                                   :ui {:label strings/offset-y
                                                        :step 0.1}}))
         :line line-style
         :geometry {:size {:type :range
                           :min 0.1
                           :max 90
                           :default 25
                           :ui {:label strings/size
                                :step 0.1}}
                    :ui {:label strings/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary-shared/add-humetty-and-voided context))))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        unadjusted-origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x unadjusted-origin-point))
        bottom (assoc (:bottom points) :x (:x unadjusted-origin-point))
        left (assoc (:left points) :y (:y unadjusted-origin-point))
        right (assoc (:right points) :y (:y unadjusted-origin-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of width)))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-width
                                     nil)
        [relative-top-left relative-top-right
         relative-bottom-left relative-bottom-right] (saltire/arm-diagonals origin-point anchor-point)
        diagonal-top-left (v/add origin-point relative-top-left)
        diagonal-top-right (v/add origin-point relative-top-right)
        diagonal-bottom-left (v/add origin-point relative-bottom-left)
        diagonal-bottom-right (v/add origin-point relative-bottom-right)
        angle-bottom-right (v/angle-to-point origin-point diagonal-bottom-right)
        angle (-> angle-bottom-right (* Math/PI) (/ 180))
        dx (/ band-width 2 (Math/sin angle))
        dy (/ band-width 2 (Math/cos angle))
        offset-top (v/v 0 (- dy))
        offset-bottom (v/v 0 dy)
        offset-left (v/v (- dx) 0)
        offset-right (v/v dx 0)
        corner-top (v/add origin-point offset-top)
        corner-bottom (v/add origin-point offset-bottom)
        corner-left (v/add origin-point offset-left)
        corner-right (v/add origin-point offset-right)
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
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
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
        shape (ordinary-shared/adjust-shape
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
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary-shared/adjusted-shape-outline
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
