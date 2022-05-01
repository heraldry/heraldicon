(ns heraldicon.heraldry.ordinary.type.label
  (:require
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.line.fimbriation :as fimbriation]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.heraldry.position :as position]
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]
   [heraldicon.util :as util]))

(def ordinary-type :heraldry.ordinary.type/label)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/label)

(defmethod interface/options ordinary-type [context]
  (let [num-points (or (interface/get-raw-data (c/++ context :num-points))
                       3)]
    (-> {:anchor {:point {:type :choice
                          :choices [[:string.option.point-choice/fess :fess]
                                    [:string.option.point-choice/chief :chief]
                                    [:string.option.point-choice/base :base]
                                    [:string.option.point-choice/honour :honour]
                                    [:string.option.point-choice/nombril :nombril]
                                    [:string.option.point-choice/top :top]
                                    [:string.option.point-choice/bottom :bottom]]
                          :default :chief
                          :ui {:label :string.option/point}}
                  :alignment {:type :choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui {:label :string.option/alignment
                                   :form-type :radio-select}}
                  :offset-y {:type :range
                             :min -45
                             :max 45
                             :default 0
                             :ui {:label :string.option/offset-y
                                  :step 0.1}}
                  :ui {:label :string.option/anchor
                       :form-type :position}}
         :variant {:type :choice
                   :choices [[:string.option.variant-label-choice/full :full]
                             [:string.option.variant-label-choice/truncated :truncated]]
                   :default :full
                   :ui {:label :string.option/variant
                        :form-type :radio-select}}
         :num-points {:type :range
                      :min 1
                      :max 16
                      :default 3
                      :integer? true
                      :ui {:label :string.option/number-of-points}}
         :geometry {:size {:type :range
                           :min 2
                           :max 90
                           :default (case num-points
                                      1 10
                                      2 10
                                      3 10
                                      4 9
                                      5 8
                                      6 7
                                      7 6
                                      5)
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :width {:type :range
                            :min 10
                            :max 150
                            :default (case num-points
                                       1 66
                                       2 66
                                       3 66
                                       4 76
                                       5 80
                                       6 84
                                       7 87
                                       90)
                            :ui {:label :string.option/width
                                 :step 0.1}}
                    :thickness {:type :range
                                :min 0
                                :max 20
                                :default 5
                                :ui {:label :string.option/bar-thickness
                                     :step 0.1}}
                    :eccentricity {:type :range
                                   :min 0
                                   :max 1
                                   :default 0
                                   :ui {:label :string.option/eccentricity
                                        :step 0.01}}
                    :stretch {:type :range
                              :min 0.33
                              :max 10
                              :default (case num-points
                                         1 2
                                         2 2
                                         3 2
                                         4 2.25
                                         5 2.5
                                         6 2.75
                                         7 3
                                         3.25)
                              :ui {:label :string.option/stretch
                                   :step 0.01}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                          (options/override-if-exists [:alignment :default] :outside))}
        (ordinary.shared/add-humetty-and-voided context)
        (options/override-if-exists [:voided :thickness :default] 25))))

(defn relative-points [points]
  (reduce (fn [result point]
            (conj result (v/add (last result) point))) [(first points)] (rest points)))

(defn draw-label [variant anchor-point num-points width band-height point-width point-height eccentricity
                  line environment context]
  (let [points (:points environment)
        left (:left points)
        right (:right points)
        extra (-> point-width
                  (/ 2)
                  (* eccentricity))
        label-start (-> anchor-point
                        :x
                        (- (/ width 2)))
        label-end (+ label-start width)
        spacing (-> width
                    (- (* num-points point-width))
                    (/ (dec num-points))
                    (+ (* 2 extra)))
        row1 (- (:y anchor-point) (/ band-height 2))
        row2 (+ row1 band-height)
        first-left (v/v (- (:x left) 20) row1)
        second-left (v/v (- (:x left) 20) row2)
        first-right (v/v (+ (:x right) 20) row1)
        second-right (v/v (+ (:x right) 20) row2)
        dynamic-points (relative-points
                        (apply concat (-> [[(v/v (- label-end extra) row2)
                                            (v/v (* 2 extra) point-height)
                                            (v/v (- (+ point-width (* 2 extra))) 0)
                                            (v/v (* 2 extra) (- point-height))]]
                                          (into
                                           (repeat (dec num-points)
                                                   [(v/v (- spacing) 0)
                                                    (v/v (* 2 extra) point-height)
                                                    (v/v (- (+ point-width (* 2 extra))) 0)
                                                    (v/v (* 2 extra) (- point-height))])))))
        projected-extra (-> extra
                            (/ point-height)
                            (* 2)
                            (* band-height))
        fixed-start-points (case variant
                             :truncated [(v/v (+ label-start extra projected-extra)
                                              row1)
                                         (v/v (- label-end extra projected-extra)
                                              row1)]
                             [first-left
                              first-right
                              second-right])
        fixed-end-points (case variant
                           :truncated [(v/v (+ label-start extra projected-extra)
                                            row1)]
                           [second-left
                            first-left])
        points (concat fixed-start-points
                       dynamic-points
                       fixed-end-points)
        lines (->> points
                   (partition 2 1)
                   (mapv (fn [[p1 p2]]
                           (line/create line
                                        p1 p2
                                        :real-start 0
                                        :real-end (v/abs (v/sub p2 p1))
                                        :context context
                                        :environment environment))))]
    {:points points
     :environment-points (-> dynamic-points
                             (conj (v/v label-start row1))
                             (conj (v/v label-end row1)))
     :lines lines
     :shape (-> ["M" (-> points
                         first
                         (v/add (-> lines first :line-start)))]
                (into (map (comp path/stitch :line) lines))
                (conj "z"))}))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [anchor (interface/get-sanitized-data (c/++ context :anchor))
        variant (interface/get-sanitized-data (c/++ context :variant))
        num-points (interface/get-sanitized-data (c/++ context :num-points))
        fimbriation (interface/get-sanitized-data (c/++ context :fimbriation))
        label-width (interface/get-sanitized-data (c/++ context :geometry :width))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        thickness (interface/get-sanitized-data (c/++ context :geometry :thickness))
        eccentricity (interface/get-sanitized-data (c/++ context :geometry :eccentricity))
        stretch (interface/get-sanitized-data (c/++ context :geometry :stretch))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        line {:type :straight
              :fimbriation fimbriation}
        anchor-point (position/calculate anchor environment :fess)
        band-height (-> thickness
                        ((util/percent-of (:width environment))))
        anchor-point (case (:alignment anchor)
                       :left (v/add anchor-point (v/v 0 (/ band-height 2)))
                       :right (v/sub anchor-point (v/v 0 (/ band-height 2)))
                       anchor-point)
        point-width (-> size
                        ((util/percent-of (:width environment))))
        point-height (* point-width stretch)
        width (:width environment)
        {:keys [lines
                shape
                points
                environment-points]} (draw-label variant
                                                 anchor-point num-points
                                                 label-width band-height point-width point-height
                                                 eccentricity
                                                 line
                                                 environment
                                                 context)
        shape (ordinary.shared/adjust-shape shape width band-height context)
        part [shape environment-points]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [line/render line lines (first points) outline? context])]))
