(ns heraldry.coat-of-arms.ordinary.type.cross
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field.shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary.interface]
   [heraldry.coat-of-arms.ordinary.shared :as ordinary.shared]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/cross)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/cross)

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
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
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x anchor-point))
        bottom (assoc (:bottom points) :x (:x anchor-point))
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        width (:width environment)
        height (:height environment)
        band-width (-> size
                       ((util/percent-of width)))
        col1 (- (:x anchor-point) (/ band-width 2))
        col2 (+ col1 band-width)
        pale-top-left (v/v col1 (-> top :y (- 10)))
        pale-bottom-left (v/v col1 (-> bottom :y (+ 10)))
        pale-top-right (v/v col2 (-> top :y (- 10)))
        pale-bottom-right (v/v col2 (-> bottom :y (+ 10)))
        row1 (- (:y anchor-point) (/ band-width 2))
        row2 (+ row1 band-width)
        fess-top-left (v/v (-> left :x (- 10)) row1)
        fess-top-right (v/v (-> right :x (+ 10)) row1)
        fess-bottom-left (v/v (-> left :x (- 10)) row2)
        fess-bottom-right (v/v (-> right :x (+ 10)) row2)
        corner-top-left (v/v col1 row1)
        corner-top-right (v/v col2 row1)
        corner-bottom-left (v/v col1 row2)
        corner-bottom-right (v/v col2 row2)
        intersection-pale-top-left (v/find-first-intersection-of-ray corner-top-left pale-top-left environment)
        intersection-pale-top-right (v/find-first-intersection-of-ray corner-top-right pale-top-right environment)
        intersection-pale-bottom-left (v/find-first-intersection-of-ray corner-bottom-left pale-bottom-left environment)
        intersection-pale-bottom-right (v/find-first-intersection-of-ray corner-bottom-right pale-bottom-right environment)
        intersection-fess-top-left (v/find-first-intersection-of-ray corner-top-left fess-top-left environment)
        intersection-fess-top-right (v/find-first-intersection-of-ray corner-top-right fess-top-right environment)
        intersection-fess-bottom-left (v/find-first-intersection-of-ray corner-bottom-left fess-bottom-left environment)
        intersection-fess-bottom-right (v/find-first-intersection-of-ray corner-bottom-right fess-bottom-right environment)
        end-pale-top-left (-> intersection-pale-top-left
                              (v/sub corner-top-left)
                              v/abs)
        end-pale-top-right (-> intersection-pale-top-right
                               (v/sub corner-top-right)
                               v/abs)
        end-pale-bottom-left (-> intersection-pale-bottom-left
                                 (v/sub corner-bottom-left)
                                 v/abs)
        end-pale-bottom-right (-> intersection-pale-bottom-right
                                  (v/sub corner-bottom-right)
                                  v/abs)
        end-fess-top-left (-> intersection-fess-top-left
                              (v/sub corner-top-left)
                              v/abs)
        end-fess-top-right (-> intersection-fess-top-right
                               (v/sub corner-top-right)
                               v/abs)
        end-fess-bottom-left (-> intersection-fess-bottom-left
                                 (v/sub corner-bottom-left)
                                 v/abs)
        end-fess-bottom-right (-> intersection-fess-bottom-right
                                  (v/sub corner-bottom-right)
                                  v/abs)
        end (max end-pale-top-left
                 end-pale-top-right
                 end-pale-bottom-left
                 end-pale-bottom-right
                 end-fess-top-left
                 end-fess-top-right
                 end-fess-bottom-left
                 end-fess-bottom-right)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-pale-top-left :line
         line-pale-top-left-start :line-start
         line-pale-top-left-min :line-min
         :as line-pale-top-left-data} (line/create line
                                                   corner-top-left pale-top-left
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        {line-pale-top-right :line
         line-pale-top-right-start :line-start
         :as line-pale-top-right-data} (line/create line
                                                    corner-top-right pale-top-right
                                                    :reversed? true
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-fess-top-right :line
         line-fess-top-right-start :line-start
         :as line-fess-top-right-data} (line/create line
                                                    corner-top-right fess-top-right
                                                    :real-start 0
                                                    :real-end end
                                                    :context context
                                                    :environment environment)
        {line-fess-bottom-right :line
         line-fess-bottom-right-start :line-start
         :as line-fess-bottom-right-data} (line/create line
                                                       corner-bottom-right fess-bottom-right
                                                       :reversed? true
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        {line-pale-bottom-right :line
         line-pale-bottom-right-start :line-start
         :as line-pale-bottom-right-data} (line/create line
                                                       corner-bottom-right pale-bottom-right
                                                       :real-start 0
                                                       :real-end end
                                                       :context context
                                                       :environment environment)
        {line-pale-bottom-left :line
         line-pale-bottom-left-start :line-start
         :as line-pale-bottom-left-data} (line/create line
                                                      corner-bottom-left pale-bottom-left
                                                      :reversed? true
                                                      :real-start 0
                                                      :real-end end
                                                      :context context
                                                      :environment environment)
        {line-fess-bottom-left :line
         line-fess-bottom-left-start :line-start
         :as line-fess-bottom-left-data} (line/create line
                                                      corner-bottom-left fess-bottom-left
                                                      :real-start 0
                                                      :real-end end
                                                      :context context
                                                      :environment environment)
        {line-fess-top-left :line
         line-fess-top-left-start :line-start
         :as line-fess-top-left-data} (line/create line
                                                   corner-top-left fess-top-left
                                                   :reversed? true
                                                   :real-start 0
                                                   :real-end end
                                                   :context context
                                                   :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add corner-top-left
                           line-pale-top-left-start)
                (path/stitch line-pale-top-left)
                (infinity/path :clockwise
                               [:top :top]
                               [(v/add pale-top-left
                                       line-pale-top-left-start)
                                (v/add pale-top-right
                                       line-pale-top-right-start)])
                (path/stitch line-pale-top-right)
                "L" (v/add corner-top-right
                           line-fess-top-right-start)
                (path/stitch line-fess-top-right)
                (infinity/path :clockwise
                               [:right :right]
                               [(v/add fess-top-right
                                       line-fess-top-right-start)
                                (v/add fess-bottom-right
                                       line-fess-bottom-right-start)])
                (path/stitch line-fess-bottom-right)
                "L" (v/add corner-bottom-right
                           line-pale-bottom-right-start)
                (path/stitch line-pale-bottom-right)
                (infinity/path :clockwise
                               [:bottom :bottom]
                               [(v/add pale-bottom-right
                                       line-pale-bottom-right-start)
                                (v/add pale-bottom-left
                                       line-pale-bottom-left-start)])
                (path/stitch line-pale-bottom-left)
                "L" (v/add corner-bottom-left
                           line-fess-bottom-left-start)
                (path/stitch line-fess-bottom-left)
                (infinity/path :clockwise
                               [:left :left]
                               [(v/add fess-bottom-left
                                       line-fess-bottom-left-start)
                                (v/add fess-top-left
                                       line-fess-top-left-start)])
                (path/stitch line-fess-top-left)
                "z"]
               width
               band-width
               context)
        part [shape
              [top-left bottom-right]]]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [:<>
       [line/render line [line-fess-top-left-data
                          line-pale-top-left-data] fess-top-left outline? context]
       [line/render line [line-pale-top-right-data
                          line-fess-top-right-data] pale-top-right outline? context]
       [line/render line [line-fess-bottom-right-data
                          line-pale-bottom-right-data] fess-bottom-right outline? context]
       [line/render line [line-pale-bottom-left-data
                          line-fess-bottom-left-data] pale-bottom-left outline? context]])
     [:<>
      (for [[chevron-angle
             corner-point] [[225 corner-top-left]
                            [315 corner-top-right]
                            [135 corner-bottom-left]
                            [45 corner-bottom-right]]]
        ^{:key chevron-angle}
        [cottising/render-chevron-cottise
         (c/++ context :cottising :cottise-1)
         :cottise-2 :cottise-opposite-1
         :distance-fn (fn [distance half-joint-angle-rad]
                        (-> (+ distance)
                            (/ 100)
                            (* width)
                            (- line-pale-top-left-min)
                            (/ (if (zero? half-joint-angle-rad)
                                 0.00001
                                 (Math/sin half-joint-angle-rad)))))
         :alignment :right
         :width width
         :height height
         :chevron-angle chevron-angle
         :joint-angle 90
         :corner-point corner-point
         :swap-lines? true])]]))
