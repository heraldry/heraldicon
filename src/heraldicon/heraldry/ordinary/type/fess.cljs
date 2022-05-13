(ns heraldicon.heraldry.ordinary.type.fess
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/fess)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/fess)

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (-> {:anchor {:point {:type :choice
                          :choices [[:string.option.point-choice/fess :fess]
                                    [:string.option.point-choice/chief :chief]
                                    [:string.option.point-choice/base :base]
                                    [:string.option.point-choice/honour :honour]
                                    [:string.option.point-choice/nombril :nombril]
                                    [:string.option.point-choice/top :top]
                                    [:string.option.point-choice/bottom :bottom]]
                          :default :fess
                          :ui {:label :string.option/point}}
                  :alignment {:type :choice
                              :choices position/alignment-choices
                              :default :middle
                              :ui {:label :string.option/alignment
                                   :form-type :radio-select}}
                  :offset-y {:type :range
                             :min -75
                             :max 75
                             :default 0
                             :ui {:label :string.option/offset-y
                                  :step 0.1}}
                  :ui {:label :string.option/anchor
                       :form-type :position}}
         :line line-style
         :opposite-line opposite-line-style
         :geometry {:size {:type :range
                           :min 0.1
                           :max 90
                           :default 25
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 2)}
        (ordinary.shared/add-humetty-and-voided context))))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment
           override-real-start
           override-real-end
           override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        left (assoc (:left points) :y (:y anchor-point))
        right (assoc (:right points) :y (:y anchor-point))
        width (:width environment)
        height (:height environment)
        band-height (-> size
                        ((math/percent-of height)))
        row1 (case (:alignment anchor)
               :left (:y anchor-point)
               :right (- (:y anchor-point) band-height)
               (- (:y anchor-point) (/ band-height 2)))
        row2 (+ row1 band-height)
        first-left (v/Vector. (:x left) row1)
        first-right (v/Vector. (:x right) row1)
        second-left (v/Vector. (:x left) row2)
        second-right (v/Vector. (:x right) row2)
        [first-real-left _first-real-right] (v/environment-intersections
                                             first-left
                                             first-right
                                             environment)
        [second-real-left _second-real-right] (v/environment-intersections
                                               second-left
                                               second-right
                                               environment)
        shared-start-x (or override-shared-start-x
                           (- (min (:x first-real-left)
                                   (:x second-real-left))
                              30))
        real-start (or override-real-start
                       (min (-> first-left :x (- shared-start-x))
                            (-> second-left :x (- shared-start-x))))
        real-end (or override-real-end
                     (max (-> first-right :x (- shared-start-x))
                          (-> second-right :x (- shared-start-x))))
        shared-end-x (+ real-end 30)
        first-left (v/Vector. shared-start-x (:y first-left))
        second-left (v/Vector. shared-start-x (:y second-left))
        first-right (v/Vector. shared-end-x (:y first-right))
        second-right (v/Vector. shared-end-x (:y second-right))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (math/percent-of height))
                 (update-in [:fimbriation :thickness-2] (math/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (math/percent-of height))
                          (update-in [:fimbriation :thickness-2] (math/percent-of height)))
        {line-one :line
         line-one-start :line-start
         line-one-min :line-min
         :as line-one-data} (line/create line
                                         first-left first-right
                                         :real-start real-start
                                         :real-end real-end
                                         :context context
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create opposite-line
                                              second-left second-right
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :context context
                                              :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add first-left
                           line-one-start)
                (path/stitch line-one)
                (infinity/path :clockwise
                               [:right :right]
                               [(v/add first-right
                                       line-one-start)
                                (v/add second-right
                                       line-reversed-start)])
                (path/stitch line-reversed)
                (infinity/path :clockwise
                               [:left :left]
                               [(v/add second-left
                                       line-reversed-start)
                                (v/add first-left
                                       line-one-start)])
                "z"]
               width
               band-height
               context)
        part [shape
              [(v/Vector. (:x right)
                          (:y first-right))
               (v/Vector. (:x left)
                          (:y second-left))]]
        cottise-context (merge
                         context
                         {:override-shared-start-x shared-start-x
                          :override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field.shared/make-subfield
      (c/++ context :field)
      part
      :all]
     (ordinary.shared/adjusted-shape-outline
      shape outline? context
      [:<>
       [line/render line [line-one-data] first-left outline? context]
       [line/render opposite-line [line-reversed-data] second-right outline? context]])
     [cottising/render-fess-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row1)
                         (- line-one-min)
                         (/ height)
                         (* 100)
                         (+ distance)))
      :alignment :right]

     [cottising/render-fess-cottise
      (c/++ context :cottising :cottise-opposite-1)
      :cottise-opposite-2 :cottise-opposite-1
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row2)
                         (+ line-reversed-min)
                         (/ height)
                         (* 100)
                         (- distance)))
      :alignment :left
      :swap-lines? true]]))
