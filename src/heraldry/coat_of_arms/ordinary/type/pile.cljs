(ns heraldry.coat-of-arms.ordinary.type.pile
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary-shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.coat-of-arms.shared.pile :as pile]
   [heraldry.context :as c]
   [heraldry.gettext :refer [string]]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/pile)

(defmethod ordinary-interface/display-name ordinary-type [_] (string "Pile"))

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:offset :min] 0)
                                (options/override-if-exists [:base-line] nil)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))
        origin-point-option {:type :choice
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
                                       [(string "Bottom-right") :bottom-right]]
                             :default :top
                             :ui {:label (string "Point")}}
        current-origin-point (options/get-value
                              (interface/get-raw-data (c/++ context :origin :point))
                              origin-point-option)
        anchor-point-option {:type :choice
                             :choices (util/filter-choices
                                       [[(string "Top-left") :top-left]
                                        [(string "Top") :top]
                                        [(string "Top-right") :top-right]
                                        [(string "Left") :left]
                                        [(string "Right") :right]
                                        [(string "Bottom-left") :bottom-left]
                                        [(string "Bottom") :bottom]
                                        [(string "Bottom-right") :bottom-right]
                                        [(string "Fess [point]") :fess]
                                        [(string "Chief [point]") :chief]
                                        [(string "Base [point]") :base]
                                        [(string "Dexter [point]") :dexter]
                                        [(string "Sinister [point]") :sinister]
                                        [(string "Honour [point]") :honour]
                                        [(string "Nombril [point]") :nombril]
                                        [(string "Angle") :angle]]
                                       #(not= % current-origin-point))
                             :default :fess
                             :ui {:label (string "Point")}}
        current-anchor-point (options/get-value
                              (interface/get-raw-data (c/++ context :anchor :point))
                              anchor-point-option)
        size-mode-option {:type :choice
                          :choices [[(string "Thickness") :thickness]
                                    [(string "Angle") :angle]]
                          :default :thickness
                          :ui {:label (string "Size mode")
                               :form-type :radio-select}}
        current-size-mode (options/get-value
                           (interface/get-raw-data (c/++ context :geometry :size-mode))
                           size-mode-option)]
    (-> {:origin {:point origin-point-option
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
         :anchor (cond-> {:point anchor-point-option
                          :ui {:label (string "Anchor")
                               :form-type :position}}

                   (= current-anchor-point
                      :angle) (assoc :angle {:type :range
                                             :min (cond
                                                    (#{:top-left
                                                       :top-right
                                                       :bottom-left
                                                       :bottom-right} current-origin-point) 0
                                                    :else -90)
                                             :max 90
                                             :default (cond
                                                        (#{:top-left
                                                           :top-right
                                                           :bottom-left
                                                           :bottom-right} current-origin-point) 45
                                                        :else 0)
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
                                                        :step 0.1}}
                                        :type {:type :choice
                                               :choices [[(string "Edge") :edge]
                                                         [(string "Anchor-Point") :point]]
                                               :default :edge
                                               :ui {:label (string "Mode")
                                                    :form-type :radio-select}}))
         :line line-style
         :opposite-line opposite-line-style
         :geometry {:size-mode size-mode-option
                    :size {:type :range
                           :min 5
                           :max 120
                           :default (case current-size-mode
                                      :thickness 75
                                      30)
                           :ui {:label (string "Size")
                                :step 0.1}}
                    :stretch {:type :range
                              :min 0.33
                              :max 2
                              :default 1
                              :ui {:label (string "Stretch")
                                   :step 0.01}}
                    :ui {:label (string "Geometry")
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary-shared/add-humetty-and-voided context))))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        origin (interface/get-sanitized-data (c/++ context :origin))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        geometry (interface/get-sanitized-data (c/++ context :geometry))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)
        bottom-left (:bottom-left points)
        bottom-right (:bottom-right points)
        width (:width environment)
        height (:height environment)
        thickness-base (if (#{:left :right} (:point origin))
                         height
                         width)
        {origin-point :origin
         point :point
         thickness :thickness} (pile/calculate-properties
                                environment
                                origin
                                (cond-> anchor
                                  (#{:top-right
                                     :right
                                     :bottom-left} (:point origin)) (update :angle #(when %
                                                                                      (- %))))
                                geometry
                                thickness-base
                                (case (:point origin)
                                  :top-left 0
                                  :top 90
                                  :top-right 180
                                  :left 0
                                  :right 180
                                  :bottom-left 0
                                  :bottom -90
                                  :bottom-right 180
                                  0))
        pile-angle (v/angle-to-point point origin-point)
        {left-point :left
         right-point :right} (pile/diagonals origin-point point thickness)
        intersection-left (-> (v/environment-intersections point left-point environment)
                              last)
        intersection-right (-> (v/environment-intersections point right-point environment)
                               last)
        joint-angle (v/angle-between-vectors (v/sub intersection-left point)
                                             (v/sub intersection-right point))
        end-left (-> intersection-left
                     (v/sub point)
                     v/abs)
        end-right (-> intersection-right
                      (v/sub point)
                      v/abs)
        end (max end-left end-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of thickness-base))
                 (update-in [:fimbriation :thickness-2] (util/percent-of thickness-base)))
        {line-left :line
         line-left-start :line-start
         line-left-min :line-min
         :as line-left-data} (line/create line
                                          point left-point
                                          :reversed? true
                                          :real-start 0
                                          :real-end end
                                          :context context
                                          :environment environment)
        {line-right :line
         :as line-right-data} (line/create opposite-line
                                           point right-point
                                           :real-start 0
                                           :real-end end
                                           :context context
                                           :environment environment)
        shape (ordinary-shared/adjust-shape
               ["M" (v/add left-point
                           line-left-start)
                (path/stitch line-left)
                (path/stitch line-right)
                "z"]
               width
               thickness
               context)
        part [shape
              [top-left top-right
               bottom-left bottom-right]]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary-shared/adjusted-shape-outline
      shape outline? context
      [line/render line [line-left-data
                         line-right-data] left-point outline? context])
     [cottising/render-chevron-cottise
      (c/++ context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :distance-fn (fn [distance half-joint-angle-rad]
                     (-> (- distance)
                         (/ 100)
                         (* thickness-base)
                         (+ line-left-min)
                         (/ (if (zero? half-joint-angle-rad)
                              0.00001
                              (Math/sin half-joint-angle-rad)))))
      :alignment :left
      :width width
      :height height
      :chevron-angle pile-angle
      :joint-angle joint-angle
      :corner-point point]]))
