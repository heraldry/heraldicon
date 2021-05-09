(ns heraldry.coat-of-arms.ordinary.type.pale
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
  {:display-name "Pale"
   :value        :heraldry.ordinary.type/pale}
  [{:keys [field hints] :as ordinary} parent environment
   {:keys [render-options override-real-start override-real-end override-shared-start-y] :as context}]
  (let [;; ignore offset-x constraints, because cottises might exceed them
        ordinary-options                         (-> (ordinary-options/options ordinary)
                                                     (assoc-in [:origin :offset-x :min] -100)
                                                     (assoc-in [:origin :offset-x :max] 100)
                                                     (assoc-in [:origin :offset-y :min] -100)
                                                     (assoc-in [:origin :offset-y :max] 100))
        {:keys [line origin geometry]}           (options/sanitize ordinary ordinary-options)
        opposite-line                            (ordinary-options/sanitize-opposite-line ordinary line)
        {:keys [size]}                           geometry
        points                                   (:points environment)
        plain-origin                             (get points (:point origin))
        origin-point                             (position/calculate origin environment :fess)
        top                                      (assoc (:top points) :x (:x origin-point))
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        width                                    (:width environment)
        band-width                               (-> size
                                                     ((util/percent-of width)))
        col1                                     (case (:alignment origin)
                                                   :left  (:x origin-point)
                                                   :right (- (:x origin-point) band-width)
                                                   (- (:x origin-point) (/ band-width 2)))
        col2                                     (+ col1 band-width)
        first-top                                (v/v col1 (:y top))
        first-bottom                             (v/v col1 (:y bottom))
        second-top                               (v/v col2 (:y top))
        second-bottom                            (v/v col2 (:y bottom))
        [first-top first-bottom]                 (v/environment-intersections
                                                  first-top
                                                  first-bottom
                                                  environment)
        [second-top second-bottom]               (v/environment-intersections
                                                  second-top
                                                  second-bottom
                                                  environment)
        shared-start-y                           (or override-shared-start-y
                                                     (- (min (:y first-top)
                                                             (:y second-top))
                                                        30))
        real-start                               (or override-real-start
                                                     (min (-> first-top :y (- shared-start-y))
                                                          (-> second-top :y (- shared-start-y))))
        real-end                                 (or override-real-end
                                                     (max (-> first-bottom :y (- shared-start-y))
                                                          (-> second-bottom :y (- shared-start-y))))
        shared-end-y                             (+ real-end 30)
        first-top                                (v/v (:x first-top) shared-start-y)
        second-top                               (v/v (:x second-top) shared-start-y)
        first-bottom                             (v/v (:x first-bottom) shared-end-y)
        second-bottom                            (v/v (:x second-bottom) shared-end-y)
        line                                     (-> line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        opposite-line                            (-> opposite-line
                                                     (update-in [:fimbriation :thickness-1] (util/percent-of width))
                                                     (update-in [:fimbriation :thickness-2] (util/percent-of width)))
        {line-one       :line
         line-one-start :line-start
         line-one-min   :line-min
         :as            line-one-data}           (line/create line
                                                              first-top first-bottom
                                                              :reversed? true
                                                              :real-start real-start
                                                              :real-end real-end
                                                              :render-options render-options
                                                              :environment environment)
        {line-reversed       :line
         line-reversed-start :line-start
         line-reversed-min   :line-min
         :as                 line-reversed-data} (line/create opposite-line
                                                              second-top second-bottom
                                                              :real-start real-start
                                                              :real-end real-end
                                                              :render-options render-options
                                                              :environment environment)
        parts                                    [[["M" (v/+ first-bottom
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
                                                    second-top]]]
        field                                    (if (:counterchanged? field)
                                                   (counterchange/counterchange-field ordinary parent)
                                                   field)
        outline?                                 (or (:outline? render-options)
                                                     (:outline? hints))
        {:keys [cottise-1
                cottise-2
                cottise-opposite-1
                cottise-opposite-2]}             (-> ordinary :cottising)]
    [:<>
     [field-shared/make-subfields
      :ordinary-pale [field] parts
      [:all]
      environment ordinary context]
     (line/render line [line-one-data] first-bottom outline? render-options)
     (line/render opposite-line [line-reversed-data] second-top outline? render-options)
     (when (:enabled? cottise-1)
       (let [cottise-1-data (options/sanitize cottise-1 cottising/cottise-options)]
         [render (-> ordinary
                     (assoc :cottising {:cottise-1 cottise-2})
                     (assoc :line (:line cottise-1))
                     (assoc :opposite-line (:opposite-line cottise-1))
                     (assoc :field (:field cottise-1))
                     (assoc-in [:geometry :size] (:thickness cottise-1-data))
                     (assoc-in [:origin :offset-x] (-> plain-origin
                                                       :x
                                                       (- col1)
                                                       (- line-one-min)
                                                       (/ width)
                                                       (* 100)
                                                       (+ (:distance cottise-1-data))
                                                       -))
                     (assoc-in [:origin :alignment] :right)) parent environment
          (-> context
              (assoc :override-shared-start-y shared-start-y)
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))
     (when (:enabled? cottise-opposite-1)
       (let [cottise-opposite-1-data (options/sanitize cottise-opposite-1 cottising/cottise-options)
             pale-base               {:type          :heraldry.ordinary.type/pale
                                      :line          (:line cottise-opposite-1)
                                      :opposite-line (:opposite-line cottise-opposite-1)}
             pale-options            (ordinary-options/options pale-base)
             {:keys [line]}          (options/sanitize pale-base pale-options)
             opposite-line           (ordinary-options/sanitize-opposite-line pale-base line)]
         [render (-> ordinary
                     (assoc :cottising {:cottise-opposite-1 cottise-opposite-2})
                     ;; swap line/opposite-line because the cottise fess is upside down
                     (assoc :line opposite-line)
                     (assoc :opposite-line line)
                     (assoc :field (:field cottise-opposite-1))
                     (assoc-in [:geometry :size] (:thickness cottise-opposite-1-data))
                     (assoc-in [:origin :offset-x] (-> plain-origin
                                                       :x
                                                       (- col2)
                                                       (+ line-reversed-min)
                                                       (/ width)
                                                       (* 100)
                                                       (- (:distance cottise-opposite-1-data))
                                                       -))
                     (assoc-in [:origin :alignment] :left)) parent environment
          (-> context
              (assoc :override-shared-start-y shared-start-y)
              (assoc :override-real-start real-start)
              (assoc :override-real-end real-end))]))]))

