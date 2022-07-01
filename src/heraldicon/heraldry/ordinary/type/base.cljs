(ns heraldicon.heraldry.ordinary.type.base
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.cottising :as cottising]
   [heraldicon.heraldry.field.shared :as field.shared]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.interface :as ordinary.interface]
   [heraldicon.heraldry.ordinary.shared :as ordinary.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.infinity :as infinity]
   [heraldicon.svg.path :as path]))

(def ordinary-type :heraldry.ordinary.type/base)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/base)

(defmethod ordinary.interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (ordinary.shared/add-humetty-and-voided
     {:line line-style
      :geometry {:size {:type :range
                        :min 0.1
                        :max 75
                        :default 25
                        :ui {:label :string.option/size
                             :step 0.1}}
                 :ui {:label :string.option/geometry
                      :form-type :ui.element/geometry}}
      :outline? options/plain-outline?-option
      :cottising (cottising/add-cottising context 1)} context)))

(defmethod ordinary.interface/render-ordinary ordinary-type
  [{:keys [environment
           override-real-start
           override-real-end
           override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        size (interface/get-sanitized-data (c/++ context :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        bottom (:bottom points)
        bottom-right (:bottom-right points)
        left (:left points)
        right (:right points)
        width (:width environment)
        height (:height environment)
        band-height (math/percent-of height size)
        row (- (:y bottom) band-height)
        row-left (v/Vector. (:x left) row)
        row-right (v/Vector. (:x right) row)
        [row-real-left _row-real-right] (v/environment-intersections
                                         row-left
                                         row-right
                                         environment)
        shared-start-x (or override-shared-start-x
                           (- (:x row-real-left)
                              30))
        real-start (or override-real-start
                       (-> row-left :x (- shared-start-x)))
        real-end (or override-real-end
                     (-> row-right :x (- shared-start-x)))
        shared-end-x (+ real-end 30)
        row-left (v/Vector. shared-start-x (:y row-left))
        row-right (v/Vector. shared-end-x (:y row-right))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (partial math/percent-of height))
                 (update-in [:fimbriation :thickness-2] (partial math/percent-of height)))
        {line-one :line
         line-one-start :line-start
         line-reversed-min :line-min
         :as line-one-data} (line/create line
                                         row-left row-right
                                         :context context
                                         :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add row-left
                           line-one-start)
                (path/stitch line-one)
                (infinity/path :clockwise
                               [:right :left]
                               [(v/add row-right
                                       line-one-start)
                                (v/add row-left
                                       line-one-start)])
                "z"]
               width
               band-height
               context)
        part [shape
              [(v/Vector. (:x left)
                          (:y row-left))
               bottom-right]]
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
      [line/render line [line-one-data] row-left outline? context])
     [cottising/render-fess-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-1
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row)
                         (- line-reversed-min)
                         (/ height)
                         (* 100)
                         (+ distance)))
      :alignment :right]]))
