(ns heraldry.coat-of-arms.ordinary.type.base
  (:require [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/base)

(defmethod ordinary-interface/display-name ordinary-type [_] "Base")

(defmethod ordinary-interface/render-ordinary ordinary-type
  [path _parent-path environment {:keys [override-real-start
                                         override-real-end
                                         override-shared-start-x] :as context}]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        size (interface/get-sanitized-data (conj path :geometry :size) context)
        ;; cottising (interface/get-sanitized-data (conj path :cottising) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
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
        part [["M" (v/+ row-left
                        line-one-start)
               (svg/stitch line-one)
               (infinity/path :clockwise
                              [:right :left]
                              [(v/+ row-right
                                    line-one-start)
                               (v/+ row-left
                                    line-one-start)])
               "z"]
              [row-left bottom-right]]
        ;; TODO: counterchanged
        ;; field (if (:counterchanged? field)
        ;;         (counterchange/counterchange-field ordinary parent)
        ;;         field)
        #_#_{:keys [cottise-1
                    cottise-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfield
      (conj path :field) part
      :all
      environment context]
     (line/render line [line-one-data] row-left outline? context)
     #_(when (:enabled? cottise-1)
         (let [cottise-1-data (:cottise-1 cottising)]
           [fess/render {:type :heraldry.ordinary.type/fess
                         :outline? (-> ordinary :outline?)
                         :cottising {:cottise-1 cottise-2}
                         :line (:line cottise-1)
                         :opposite-line (:opposite-line cottise-1)
                         :field (:field cottise-1)
                         :geometry {:size (:thickness cottise-1-data)}
                         :origin {:point :bottom
                                  :offset-y (-> bottom
                                                :y
                                                (- row)
                                                (- line-reversed-min)
                                                (/ height)
                                                (* 100)
                                                (+ (:distance cottise-1-data)))
                                  :alignment :right}} parent environment
            (-> context
                (assoc :override-shared-start-x shared-start-x)
                (assoc :override-real-start real-start)
                (assoc :override-real-end real-end))]))]))
