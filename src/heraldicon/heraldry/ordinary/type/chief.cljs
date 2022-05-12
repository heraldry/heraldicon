(ns heraldicon.heraldry.ordinary.type.chief
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

(def ordinary-type :heraldry.ordinary.type/chief)

(defmethod ordinary.interface/display-name ordinary-type [_] :string.ordinary.type/chief)

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    (-> {:line line-style
         :geometry {:size {:type :range
                           :min 0.1
                           :max 75
                           :default 25
                           :ui {:label :string.option/size
                                :step 0.1}}
                    :ui {:label :string.option/geometry
                         :form-type :geometry}}
         :outline? options/plain-outline?-option
         :cottising (cottising/add-cottising context 1)}
        (ordinary.shared/add-humetty-and-voided context))))

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
        top (:top points)
        top-left (:top-left points)
        left (:left points)
        right (:right points)
        width (:width environment)
        height (:height environment)
        band-height (-> size
                        ((math/percent-of height)))
        row (+ (:y top) band-height)
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
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
        row-left (v/v shared-start-x (:y row-left))
        row-right (v/v shared-end-x (:y row-right))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (math/percent-of height))
                 (update-in [:fimbriation :thickness-2] (math/percent-of height)))
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
         :as line-reversed-data} (line/create line
                                              row-left row-right
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :context context
                                              :environment environment)
        shape (ordinary.shared/adjust-shape
               ["M" (v/add row-right
                           line-reversed-start)
                (path/stitch line-reversed)
                (infinity/path :clockwise
                               [:left :right]
                               [(v/add row-left
                                       line-reversed-start)
                                (v/add row-right
                                       line-reversed-start)])
                "z"]
               width
               band-height
               context)
        part [shape
              [top-left
               (v/v (:x right)
                    (:y row-right))]]
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
      [line/render line [line-reversed-data] row-right outline? context])
     [cottising/render-fess-cottise
      (c/++ cottise-context :cottising :cottise-1)
      :cottise-2 :cottise-opposite-1
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row)
                         (+ line-reversed-min)
                         (/ height)
                         (* 100)
                         (- distance)))
      :alignment :left
      :swap-lines? true]]))
