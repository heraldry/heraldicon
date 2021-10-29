(ns heraldry.coat-of-arms.ordinary.type.label
  (:require
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/label)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Label"
                                                              :de "Turnierkragen"})

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
  [path _parent-path environment context]
  (let [origin (interface/get-sanitized-data (conj path :origin) context)
        variant (interface/get-sanitized-data (conj path :variant) context)
        num-points (interface/get-sanitized-data (conj path :num-points) context)
        fimbriation (interface/get-sanitized-data (conj path :fimbriation) context)
        width (interface/get-sanitized-data (conj path :geometry :width) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        thickness (interface/get-sanitized-data (conj path :geometry :thickness) context)
        eccentricity (interface/get-sanitized-data (conj path :geometry :eccentricity) context)
        stretch (interface/get-sanitized-data (conj path :geometry :stretch) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
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
        {:keys [lines
                shape
                points
                environment-points]} (draw-label variant
                                                 origin-point num-points
                                                 width band-height point-width point-height
                                                 eccentricity
                                                 line
                                                 environment
                                                 context)
        part [shape
              environment-points]]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     [line/render line lines (first points) outline? context]]))
