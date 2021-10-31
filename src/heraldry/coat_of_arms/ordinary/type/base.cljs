(ns heraldry.coat-of-arms.ordinary.type.base
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/base)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Base"
                                                              :de "Schildfuß"})

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [path environment
           override-real-start
           override-real-end
           override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (update context :path conj :line))
        size (interface/get-sanitized-data (update context :path conj :geometry :size))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (update context :path conj :outline?)))
        points (:points environment)
        bottom (:bottom points)
        bottom-right (:bottom-right points)
        left (:left points)
        right (:right points)
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        row (- (:y bottom) band-height)
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
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        {line-one :line
         line-one-start :line-start
         line-reversed-min :line-min
         :as line-one-data} (line/create line
                                         row-left row-right
                                         :context context
                                         :environment environment)
        part [["M" (v/add row-left
                          line-one-start)
               (path/stitch line-one)
               (infinity/path :clockwise
                              [:right :left]
                              [(v/add row-right
                                      line-one-start)
                               (v/add row-left
                                      line-one-start)])
               "z"]
              [(v/v (:x left)
                    (:y row-left))
               bottom-right]]
        cottise-context (merge
                         context
                         {:override-shared-start-x shared-start-x
                          :override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field-shared/make-subfield
      (update context :path conj :field)
      part
      :all]
     [line/render line [line-one-data] row-left outline? context]
     [cottising/render-fess-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment cottise-context
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row)
                         (- line-reversed-min)
                         (/ height)
                         (* 100)
                         (+ distance)))
      :alignment :right]]))
