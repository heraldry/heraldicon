(ns heraldry.coat-of-arms.ordinary.type.bend
  (:require
   [heraldry.coat-of-arms.angle :as angle]
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/bend)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Bend"
                                                              :de "Schrägbalken"})

(defmethod interface/options ordinary-type [context]
  (let [line-data (interface/get-raw-data (c/++ context :line))
        opposite-line-data (interface/get-raw-data (c/++ context :opposite-line))
        line-style (-> (line/options line-data)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        sanitized-line (options/sanitize line-data line-style)
        opposite-line-style (-> (line/options opposite-line-data :inherited sanitized-line)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside)
                                (update :ui assoc :label strings/opposite-line))
        origin-point-option {:type :choice
                             :choices [[strings/fess-point :fess]
                                       [strings/chief-point :chief]
                                       [strings/base-point :base]
                                       [strings/honour-point :honour]
                                       [strings/nombril-point :nombril]
                                       [strings/top-left :top-left]
                                       [strings/bottom-right :bottom-right]]
                             :default :top-left
                             :ui {:label strings/point}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        anchor-point-option {:type :choice
                             :choices (case current-origin-point
                                        :top-left [[strings/fess-point :fess]
                                                   [strings/chief-point :chief]
                                                   [strings/base-point :base]
                                                   [strings/honour-point :honour]
                                                   [strings/nombril-point :nombril]
                                                   [strings/bottom-right :bottom-right]
                                                   [strings/angle :angle]]
                                        :bottom-right [[strings/fess-point :fess]
                                                       [strings/chief-point :chief]
                                                       [strings/base-point :base]
                                                       [strings/honour-point :honour]
                                                       [strings/nombril-point :nombril]
                                                       [strings/top-left :top-left]
                                                       [strings/angle :angle]]
                                        [[strings/top-left :top-left]
                                         [strings/bottom-right :bottom-right]
                                         [strings/angle :angle]])
                             :default (case current-origin-point
                                        :top-left :fess
                                        :bottom-right :fess
                                        :top-left)
                             :ui {:label strings/point}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)]
    {:origin {:point origin-point-option
              :alignment {:type :choice
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
                              :step 0.1}}
              :ui {:label strings/origin
                   :form-type :position}}
     :anchor (cond-> {:point anchor-point-option
                      :ui {:label strings/anchor
                           :form-type :position}}

               (= current-anchor-point
                  :angle) (assoc :angle {:type :range
                                         :min 0
                                         :max 360
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
     :opposite-line opposite-line-style
     :geometry {:size {:type :range
                       :min 0.1
                       :max 90
                       :default 25
                       :ui {:label strings/size
                            :step 0.1}}
                :ui {:label strings/geometry
                     :form-type :geometry}}
     :outline? options/plain-outline?-option
     :cottising (-> cottising/default-options
                    (dissoc :cottise-extra-1)
                    (dissoc :cottise-extra-2))}))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment
           override-middle-real-start
           override-middle-real-end
           override-real-start
           override-real-end
           override-center-point] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top (:top points)
        bottom (:bottom points)
        width (:width environment)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     band-height
                                     nil)
        center-point (or override-center-point
                         (v/line-intersection origin-point anchor-point
                                              top bottom))
        direction (v/sub anchor-point origin-point)
        direction (-> (v/v (-> direction :x Math/abs)
                           (-> direction :y Math/abs))
                      v/normal)
        direction-orthogonal (v/orthogonal direction)
        [middle-real-start
         middle-real-end] (if (and override-middle-real-start
                                   override-middle-real-end)
                            [override-middle-real-start
                             override-middle-real-end]
                            (v/environment-intersections
                             origin-point
                             (v/add origin-point direction)
                             environment))
        band-length (-> (v/sub middle-real-start center-point)
                        v/abs
                        (* 2))
        middle-start (-> direction
                         (v/mul -30)
                         (v/add middle-real-start))
        middle-end (-> direction
                       (v/mul 30)
                       (v/add middle-real-end))
        width-offset (-> direction-orthogonal
                         (v/mul band-height)
                         (v/div 2))
        ordinary-top-left (v/add middle-real-start width-offset)
        first-start (v/add middle-start width-offset)
        first-end (v/add middle-end width-offset)
        second-start (v/sub middle-start width-offset)
        second-end (v/sub middle-end width-offset)
        [first-real-start
         first-real-end] (v/environment-intersections
                          first-start
                          first-end
                          environment)
        [second-real-start
         second-real-end] (v/environment-intersections
                           second-start
                           second-end
                           environment)
        real-start (or override-real-start
                       (min (-> (v/sub first-real-start first-start)
                                (v/abs))
                            (-> (v/sub second-real-start second-start)
                                (v/abs))))
        real-end (or override-real-end
                     (max (-> (v/sub first-real-start first-start)
                              (v/abs))
                          (-> (v/sub second-real-end second-start)
                              (v/abs))))
        angle (v/angle-to-point middle-start middle-end)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one :line
         line-one-start :line-start
         line-one-min :line-min
         :as line-one-data} (line/create line
                                         first-start
                                         first-end
                                         :real-start real-start
                                         :real-end real-end
                                         :context context
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create opposite-line
                                              second-start
                                              second-end
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :context context
                                              :environment environment)
        counterchanged? (interface/get-sanitized-data (c/++ context :field :counterchanged?))
        inherit-environment? (interface/get-sanitized-data (c/++ context :field :inherit-environment?))
        use-parent-environment? (or counterchanged?
                                    inherit-environment?)
        part [["M" (v/add first-start
                          line-one-start)
               (path/stitch line-one)
               "L" (v/add second-end
                          line-reversed-start)
               (path/stitch line-reversed)
               "L" (v/add first-start
                          line-one-start)
               "z"]
              (if use-parent-environment?
                [first-real-start first-real-end
                 second-real-start second-real-end]
                [(v/v 0 0)
                 (v/v band-length band-height)])]
        cottise-context (merge
                         context
                         {:override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field-shared/make-subfield
      (-> context
          (c/++ :field)
          (c/<< :transform (when (not use-parent-environment?)
                             (str "translate(" (v/->str ordinary-top-left) ")"
                                  "rotate(" angle ")"))))
      part
      :all]
     [line/render line [line-one-data] first-start outline? context]
     [line/render opposite-line [line-reversed-data] second-end outline? context]
     [cottising/render-bend-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :distance-fn (fn [distance thickness]
                     (-> (+ distance)
                         (+ (/ thickness 2))
                         (/ 100)
                         (* height)
                         (+ (/ band-height 2))
                         (- line-one-min)))
      :alignment :right
      :width width
      :height height
      :angle angle
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/add middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/add middle-real-end point-offset))]
     [cottising/render-bend-cottise
      (c/++ cottise-context :cottising :cottise-opposite-1)
      :cottise-opposite-2 :cottise-opposite-1
      :distance-fn (fn [distance thickness]
                     (-> (+ distance)
                         (+ (/ thickness 2))
                         (/ 100)
                         (* height)
                         (+ (/ band-height 2))
                         (- line-reversed-min)))
      :alignment :left
      :width width
      :height height
      :angle angle
      :direction-orthogonal direction-orthogonal
      :center-point center-point
      :middle-real-start-fn (fn [point-offset]
                              (v/sub middle-real-start point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/sub middle-real-end point-offset))
      :swap-lines? true]]))
