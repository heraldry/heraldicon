(ns heraldry.coat-of-arms.ordinary.type.label
  (:require
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary-shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/label)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Label"
                                                              :de "Turnierkragen"})

(defmethod interface/options ordinary-type [context]
  (-> {:origin {:point {:type :choice
                        :choices [[strings/fess-point :fess]
                                  [strings/chief-point :chief]
                                  [strings/base-point :base]
                                  [strings/honour-point :honour]
                                  [strings/nombril-point :nombril]
                                  [strings/top :top]
                                  [strings/bottom :bottom]]
                        :default :chief
                        :ui {:label strings/point}}
                :alignment {:type :choice
                            :choices position/alignment-choices
                            :default :middle
                            :ui {:label strings/alignment
                                 :form-type :radio-select}}
                :offset-y {:type :range
                           :min -45
                           :max 45
                           :default 0
                           :ui {:label strings/offset-y
                                :step 0.1}}
                :ui {:label strings/origin
                     :form-type :position}}
       :variant {:type :choice
                 :choices [[{:en "Full"
                             :de "Durchgehend"} :full]
                           [{:en "Truncated"
                             :de "Schwebend"} :truncated]]
                 :default :full
                 :ui {:label strings/variant
                      :form-type :radio-select}}
       :num-points {:type :range
                    :min 2
                    :max 16
                    :default 3
                    :integer? true
                    :ui {:label {:en "Number of points"
                                 :de "Anzahl Lätze"}}}
       :geometry {:size {:type :range
                         :min 2
                         :max 90
                         :default 10
                         :ui {:label strings/size
                              :step 0.1}}
                  :width {:type :range
                          :min 10
                          :max 150
                          :default 66
                          :ui {:label strings/width
                               :step 0.1}}
                  :thickness {:type :range
                              :min 0
                              :max 20
                              :default 5
                              :ui {:label strings/bar-thickness
                                   :step 0.1}}
                  :eccentricity {:type :range
                                 :min 0
                                 :max 1
                                 :default 0
                                 :ui {:label strings/eccentricity
                                      :step 0.01}}
                  :stretch {:type :range
                            :min 0.33
                            :max 10
                            :default 2
                            :ui {:label strings/stretch
                                 :step 0.01}}
                  :ui {:label strings/geometry
                       :form-type :geometry}}
       :outline? options/plain-outline?-option
       :fimbriation (-> (fimbriation/options (c/++ context :fimbriation))
                        (options/override-if-exists [:alignment :default] :outside))}
      (ordinary-shared/add-humetty-and-voided context)
      (assoc-in [:voided :thickness :default] 25)))

(defn relative-points [points]
  (reduce (fn [result point]
            (conj result (v/add (last result) point))) [(first points)] (rest points)))

(defn draw-label [variant origin-point num-points width band-height point-width point-height eccentricity
                  line environment context]
  (let [points (:points environment)
        left (:left points)
        right (:right points)
        extra (-> point-width
                  (/ 2)
                  (* eccentricity))
        label-start (-> origin-point
                        :x
                        (- (/ width 2)))
        label-end (+ label-start width)
        spacing (-> width
                    (- (* num-points point-width))
                    (/ (dec num-points))
                    (+ (* 2 extra)))
        row1 (- (:y origin-point) (/ band-height 2))
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

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [origin (interface/get-sanitized-data (c/++ context :origin))
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
        origin-point (position/calculate origin environment :fess)
        band-height (-> thickness
                        ((util/percent-of (:width environment))))
        origin-point (case (:alignment origin)
                       :left (v/add origin-point (v/v 0 (/ band-height 2)))
                       :right (v/sub origin-point (v/v 0 (/ band-height 2)))
                       origin-point)
        point-width (-> size
                        ((util/percent-of (:width environment))))
        point-height (* point-width stretch)
        width (:width environment)
        {:keys [lines
                shape
                points
                environment-points]} (draw-label variant
                                                 origin-point num-points
                                                 label-width band-height point-width point-height
                                                 eccentricity
                                                 line
                                                 environment
                                                 context)
        shape (ordinary-shared/adjust-shape shape width band-height context)
        part [shape environment-points]]
    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary-shared/adjusted-shape-outline
      shape outline? context
      [line/render line lines (first points) outline? context])]))
