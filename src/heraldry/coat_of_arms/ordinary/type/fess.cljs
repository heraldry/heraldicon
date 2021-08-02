(ns heraldry.coat-of-arms.ordinary.type.fess
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/fess)

(defmethod ordinary-interface/display-name ordinary-type [_] "Fess")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment {:keys [override-real-start
                                         override-real-end
                                         override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        row1 (case (:alignment origin)
               :left (:y origin-point)
               :right (- (:y origin-point) band-height)
               (- (:y origin-point) (/ band-height 2)))
        row2 (+ row1 band-height)
        first-left (v/v (:x left) row1)
        first-right (v/v (:x right) row1)
        second-left (v/v (:x left) row2)
        second-right (v/v (:x right) row2)
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
        first-left (v/v shared-start-x (:y first-left))
        second-left (v/v shared-start-x (:y second-left))
        first-right (v/v shared-end-x (:y first-right))
        second-right (v/v shared-end-x (:y second-right))
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of height))
                 (update-in [:fimbriation :thickness-2] (util/percent-of height)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of height))
                          (update-in [:fimbriation :thickness-2] (util/percent-of height)))
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
        part [["M" (v/+ first-left
                        line-one-start)
               (svg/stitch line-one)
               (infinity/path :clockwise
                              [:right :right]
                              [(v/+ first-right
                                    line-one-start)
                               (v/+ second-right
                                    line-reversed-start)])
               (svg/stitch line-reversed)
               (infinity/path :clockwise
                              [:left :left]
                              [(v/+ second-left
                                    line-reversed-start)
                               (v/+ first-left
                                    line-one-start)])
               "z"]
              [first-right
               second-left]]
        cottise-context (merge
                         context
                         {:override-shared-start-x shared-start-x
                          :override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     [line/render line [line-one-data] first-left outline? context]
     [line/render opposite-line [line-reversed-data] second-right outline? context]
     [cottising/render-fess-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment cottise-context
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row1)
                         (- line-one-min)
                         (/ height)
                         (* 100)
                         (+ distance)))
      :alignment :right]

     [cottising/render-fess-cottise
      :cottise-opposite-1 :cottise-opposite-2 :cottise-opposite-1
      path environment cottise-context
      :offset-y-fn (fn [base distance]
                     (-> base
                         (- row2)
                         (+ line-reversed-min)
                         (/ height)
                         (* 100)
                         (- distance)))
      :alignment :left
      :swap-lines? true]]))

