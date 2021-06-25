(ns heraldry.coat-of-arms.ordinary.type.fess
  (:require [heraldry.coat-of-arms.cottising :as cottising]
            [heraldry.coat-of-arms.counterchange :as counterchange]
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
  [{:keys [field] :as ordinary} parent environment
   {:keys [render-options override-real-start override-real-end override-shared-start-x] :as context}]
  (let [;; ignore offset-y constraints, because cottises might exceed them
        ordinary-options (-> (ordinary-options/options ordinary)
                             (assoc-in [:origin :offset-y :min] -100)
                             (assoc-in [:origin :offset-y :max] 100))
        {:keys [line origin geometry outline?]} (options/sanitize ordinary ordinary-options)
        {:keys [size]} geometry
        opposite-line (ordinary-options/sanitize-opposite-line ordinary line)
        points (:points environment)
        plain-origin (get points (:point origin))
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
                                         :render-options render-options
                                         :environment environment)
        {line-reversed :line
         line-reversed-start :line-start
         line-reversed-min :line-min
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
                     outline?)
        {:keys [cottise-1
                cottise-2
                cottise-opposite-1
                cottise-opposite-2]} (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-fess [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] first-left outline? render-options)
     (line/render opposite-line [line-reversed-data] second-right outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (options/sanitize cottise-1 cottising/cottise-options)]
         [render (-> ordinary
                     (assoc :cottising {:cottise-1 cottise-2})
                     (assoc :line (:line cottise-1))
                     (assoc :opposite-line (:opposite-line cottise-1))
                     (assoc :field (:field cottise-1))
                     (assoc-in [:geometry :size] (:thickness cottise-1-data))
                     (assoc-in [:origin :offset-y] (-> plain-origin
                                                       :y
                                                       (- row1)
                                                       (- line-one-min)
                                                       (/ height)
                                                       (* 100)
                                                       (+ (:distance cottise-1-data))))
                     (assoc-in [:origin :alignment] :right)) parent environment
          (-> context
              (assoc :override-shared-start-x shared-start-x)
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))
     (when (:enabled? cottise-opposite-1)
       (let [cottise-opposite-1-data (options/sanitize cottise-opposite-1 cottising/cottise-options)
             fess-base {:type :heraldry.ordinary.type/fess
                        :line (:line cottise-opposite-1)
                        :opposite-line (:opposite-line cottise-opposite-1)}
             fess-options (ordinary-options/options fess-base)
             {:keys [line]} (options/sanitize fess-base fess-options)
             opposite-line (ordinary-options/sanitize-opposite-line fess-base line)]
         [render (-> ordinary
                     (assoc :cottising {:cottise-opposite-1 cottise-opposite-2})
                     ;; swap line/opposite-line because the cottise fess is upside down
                     (assoc :line opposite-line)
                     (assoc :opposite-line line)
                     (assoc :field (:field cottise-opposite-1))
                     (assoc-in [:geometry :size] (:thickness cottise-opposite-1-data))
                     (assoc-in [:origin :offset-y] (-> plain-origin
                                                       :y
                                                       (- row2)
                                                       (+ line-reversed-min)
                                                       (/ height)
                                                       (* 100)
                                                       (- (:distance cottise-opposite-1-data))))
                     (assoc-in [:origin :alignment] :left)) parent environment
          (-> context
              (assoc :override-shared-start-x shared-start-x)
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))]))
