(ns heraldry.coat-of-arms.ordinary.type.fess
  (:require [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.ordinary.options :as ordinary-options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render
  {:display-name "Fess"
   :value :heraldry.ordinary.type/fess}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry
                cottising]} (options/sanitize ordinary (ordinary-options/options ordinary))
        {:keys [size]} geometry
        opposite-line (ordinary-options/sanitize-opposite-line ordinary line)
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        height (:height environment)
        band-height (-> size
                        ((util/percent-of height)))
        row1 (- (:y origin-point) (/ band-height 2))
        row2 (+ row1 band-height)
        [first-left first-right] (v/environment-intersections
                                  (v/v (:x left) row1)
                                  (v/v (:x right) row1)
                                  environment)
        [second-left second-right] (v/environment-intersections
                                    (v/v (:x left) row2)
                                    (v/v (:x right) row2)
                                    environment)
        shared-start-x (- (min (:x first-left)
                               (:x second-left))
                          30)
        real-start (min (-> first-left :x (- shared-start-x))
                        (-> second-left :x (- shared-start-x)))
        real-end (max (-> first-right :x (- shared-start-x))
                      (-> second-right :x (- shared-start-x)))
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
         :as line-one-data} (line/create line
                                         first-left first-right
                                         :real-start real-start
                                         :real-end real-end
                                         :render-options render-options
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         :as line-reversed-data} (line/create opposite-line
                                              second-left second-right
                                              :reversed? true
                                              :real-start real-start
                                              :real-end real-end
                                              :render-options render-options
                                              :environment environment)
        parts [[["M" (v/+ first-left
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
                 second-left]]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field ordinary parent)
                field)
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    (js/console.log "toc" cottising)
    [:<>
     [field-shared/make-subfields
      :ordinary-fess [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] first-left outline? render-options)
     (line/render opposite-line [line-reversed-data] second-right outline? render-options)]))
