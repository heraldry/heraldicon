(ns heraldry.coat-of-arms.ordinary.type.pale
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

(def ordinary-type :heraldry.ordinary.type/pale)

(defmethod ordinary-interface/display-name ordinary-type [_] "Pale")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment {:keys [override-real-start
                                         override-real-end
                                         override-shared-start-y] :as context}]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        opposite-line (interface/get-sanitized-data (conj path :opposite-line) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        bottom (assoc (:bottom points) :x (:x origin-point))
        width (:width environment)
        band-width (-> size
                       ((util/percent-of width)))
        col1 (case (:alignment origin)
               :left (:x origin-point)
               :right (- (:x origin-point) band-width)
               (- (:x origin-point) (/ band-width 2)))
        col2 (+ col1 band-width)
        first-top (v/v col1 (:y top))
        first-bottom (v/v col1 (:y bottom))
        second-top (v/v col2 (:y top))
        second-bottom (v/v col2 (:y bottom))
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
        first-top (v/v (:x first-top) shared-start-y)
        second-top (v/v (:x second-top) shared-start-y)
        first-bottom (v/v (:x first-bottom) shared-end-y)
        second-bottom (v/v (:x second-bottom) shared-end-y)
        line (-> line
                 (update-in [:fimbriation :thickness-1] (util/percent-of width))
                 (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        opposite-line (-> opposite-line
                          (update-in [:fimbriation :thickness-1] (util/percent-of width))
                          (update-in [:fimbriation :thickness-2] (util/percent-of width)))
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
        part [["M" (v/+ first-bottom
                        line-one-start)
               (svg/stitch line-one)
               (infinity/path :clockwise
                              [:top :top]
                              [(v/+ first-top
                                    line-one-start)
                               (v/+ second-top
                                    line-reversed-start)])
               (svg/stitch line-reversed)
               (infinity/path :clockwise
                              [:bottom :bottom]
                              [(v/+ second-bottom
                                    line-reversed-start)
                               (v/+ first-bottom
                                    line-one-start)])
               "z"]
              [first-bottom
               second-top]]
        ;; TODO: counterchanged
        ;; field (if (:counterchanged? field)
        ;;         (counterchange/counterchange-field ordinary parent)
        ;;         field)
        cottise-context (merge
                         context
                         {:override-shared-start-y shared-start-y
                          :override-real-start real-start
                          :override-real-end real-end})]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     (line/render line [line-one-data] first-bottom outline? context)
     (line/render opposite-line [line-reversed-data] second-top outline? context)
     [cottising/render-pale-cottise
      :cottise-1 :cottise-2 :cottise-1
      path environment cottise-context
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
      :cottise-opposite-1 :cottise-opposite-2 :cottise-opposite-1
      path environment cottise-context
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
