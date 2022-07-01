(ns heraldicon.heraldry.ordinary.type.pale
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

(def ordinary-type :heraldry.ordinary.type/pale)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/pale)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))
        opposite-line-style (-> (line/options (c/++ context :opposite-line) :inherited-options line-style)
                                (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:anchor {:point {:type :option.type/choice
                       :choices (position/anchor-choices
                                 [:fess
                                  :dexter
                                  :sinister
                                  :hoist
                                  :fly
                                  :left
                                  :right])
                       :default :fess
                       :ui/label :string.option/point}
               :alignment {:type :option.type/choice
                           :choices position/alignment-choices
                           :default :middle
                           :ui/label :string.option/alignment
                           :ui/element :ui.element/radio-select}
               :offset-x {:type :option.type/range
                          :min -50
                          :max 50
                          :default 0
                          :ui/label :string.option/offset-x
                          :ui/step 0.1}
               :ui/label :string.option/anchor
               :ui/element :ui.element/position}
      :line line-style
      :opposite-line opposite-line-style
      :geometry {:size {:type :option.type/range
                        :min 0.1
                        :max 90
                        :default 25
                        :ui/label :string.option/size
                        :ui/step 0.1}
                 :ui/label :string.option/geometry
                 :ui/element :ui.element/geometry}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 2)} context)))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment
           override-real-start
           override-real-end
           override-shared-start-y] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        opposite-line (interface/get-sanitized-data (c/++ context :opposite-line))
        anchor (interface/get-sanitized-data (c/++ context :anchor))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        anchor-point (position/calculate anchor environment :fess)
        top (assoc (:top points) :x (:x anchor-point))
        bottom (assoc (:bottom points) :x (:x anchor-point))
        width (:width environment)
        band-width (math/percent-of width size)
        col1 (case (:alignment anchor)
               :left (:x anchor-point)
               :right (- (:x anchor-point) band-width)
               (- (:x anchor-point) (/ band-width 2)))
        col2 (+ col1 band-width)
        first-top (v/Vector. col1 (:y top))
        first-bottom (v/Vector. col1 (:y bottom))
        second-top (v/Vector. col2 (:y top))
        second-bottom (v/Vector. col2 (:y bottom))
        [first-top first-bottom] (v/environment-intersections
                                  first-top
                                  first-bottom
                                  environment)
        [second-top second-bottom] (v/environment-intersections
                                    second-top
                                    second-bottom
                                    environment)
        shared-start-y (or override-shared-start-y
                           (- (min (:y first-top)
                                   (:y second-top))
                              30))
        real-start (or override-real-start
                       (min (-> first-top :y (- shared-start-y))
                            (-> second-top :y (- shared-start-y))))
        real-end (or override-real-end
                     (max (-> first-bottom :y (- shared-start-y))
                          (-> second-bottom :y (- shared-start-y))))
        shared-end-y (+ real-end 30)
        first-top (v/Vector. (:x first-top) shared-start-y)
        second-top (v/Vector. (:x second-top) shared-start-y)
        first-bottom (v/Vector. (:x first-bottom) shared-end-y)
        second-bottom (v/Vector. (:x second-bottom) shared-end-y)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (partial math/percent-of width))
                 (update-in [:fimbriation :thickness-2] (partial math/percent-of width)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (partial math/percent-of width))
                          (update-in [:fimbriation :thickness-2] (partial math/percent-of width)))
        {line-one :line
         line-one-start :line-start
         line-one-min :line-min
         :as line-one-data} (line/create line
                                         first-top first-bottom
                                         :reversed? true
                                         :real-start real-start
                                         :real-end real-end
                                         :context context
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create opposite-line
                                              second-top second-bottom
                                              :real-start real-start
                                              :real-end real-end
                                              :context context
                                              :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add first-bottom
                           line-one-start)
                (path/stitch line-one)
                (infinity/path :clockwise
                               [:top :top]
                               [(v/add first-top
                                       line-one-start)
                                (v/add second-top
                                       line-reversed-start)])
                (path/stitch line-reversed)
                (infinity/path :clockwise
                               [:bottom :bottom]
                               [(v/add second-bottom
                                       line-reversed-start)
                                (v/add first-bottom
                                       line-one-start)])
                "z"]
               width
               band-width
               context)
        part [shape
              [(v/Vector. (:x second-top)
                          (:y top))
               (v/Vector. (:x first-bottom)
                          (:y bottom))]]
        cottise-context (merge
                         context
                         {:override-shared-start-y shared-start-y
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
       [line/render line [line-one-data] first-bottom outline? context]
       [line/render opposite-line [line-reversed-data] second-top outline? context]])
     [cottising/render-pale-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :offset-x-fn (fn [base distance]
                     (-> base
                         (- col1)
                         (- line-one-min)
                         (/ width)
                         (* 100)
                         (+ distance)
                         -))
      :alignment :right]
     [cottising/render-pale-cottise
      (c/++ cottise-context :cottising :cottise-opposite-1)
      :cottise-opposite-2 :cottise-opposite-1
      :offset-x-fn (fn [base distance]
                     (-> base
                         (- col2)
                         (+ line-reversed-min)
                         (/ width)
                         (* 100)
                         (- distance)
                         -))
      :alignment :left
      :swap-lines? true]]))
