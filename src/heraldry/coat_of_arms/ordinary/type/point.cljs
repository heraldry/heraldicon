(ns heraldry.coat-of-arms.ordinary.type.point
  (:require
   [heraldry.coat-of-arms.cottising :as cottising]
   [heraldry.coat-of-arms.field.shared :as field-shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.line.core :as line]
   [heraldry.coat-of-arms.ordinary.interface :as ordinary-interface]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.options :as options]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def ordinary-type :heraldry.ordinary.type/point)

(defmethod ordinary-interface/display-name ordinary-type [_] {:en "Point"
                                                              :de "Schrägeck"})

(defmethod interface/options ordinary-type [context]
  (let [line-style (-> (line/options (c/++ context :line))
                       (options/override-if-exists [:offset :min] 0)
                       (options/override-if-exists [:base-line] nil)
                       (options/override-if-exists [:fimbriation :alignment :default] :outside))]
    {:line line-style
     :variant {:type :choice
               :choices [[strings/dexter :dexter]
                         [strings/sinister :sinister]]
               :default :dexter
               :ui {:label strings/variant
                    :form-type :select}}
     :geometry {:width {:type :range
                        :min 10
                        :max 100
                        :default 50
                        :ui {:label strings/width}}
                :height {:type :range
                         :min 10
                         :max 100
                         :default 50
                         :ui {:label strings/height}}
                :ui {:label strings/geometry
                     :form-type :geometry}}
     :outline? options/plain-outline?-option
     :cottising (cottising/add-cottising context 1)}))

(defmethod ordinary-interface/render-ordinary ordinary-type
  [{:keys [environment] :as context}]
  (let [line (interface/get-sanitized-data (c/++ context :line))
        variant (interface/get-sanitized-data (c/++ context :variant))
        point-width (interface/get-sanitized-data (c/++ context :geometry :width))
        point-height (interface/get-sanitized-data (c/++ context :geometry :height))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        width (:width environment)
        height (:height environment)
        points (:points environment)
        top-left (:top-left points)
        top-right (:top-right points)

        real-point-width ((util/percent-of width) point-width)
        real-point-height ((util/percent-of width) point-height)

        ideal-point-side (v/v (if (= variant :dexter)
                                (-> top-left :x)
                                (-> top-right :x))
                              (-> top-left
                                  :y
                                  (+ real-point-height)))
        ideal-point-top (v/v (if (= variant :dexter)
                               (-> top-left :x (+ real-point-width))
                               (-> top-right :x (- real-point-width)))
                             (-> top-left :y))

        extra-length 30
        line-dir (v/sub ideal-point-side ideal-point-top)
        line-dir-normed (v/normal line-dir)
        real-point-side (-> line-dir-normed
                            (v/mul extra-length)
                            (v/add ideal-point-side))
        real-point-top (-> line-dir-normed
                           (v/mul (- extra-length))
                           (v/add ideal-point-top))

        {line-one :line
         line-one-start :line-start
         :as line-one-data
         line-one-min :line-min} (if (= variant :dexter)
                                   (line/create line
                                                real-point-top real-point-side
                                                :real-start (-> ideal-point-top
                                                                (v/sub real-point-top)
                                                                v/abs)
                                                :real-end (-> ideal-point-side
                                                              (v/sub real-point-top)
                                                              v/abs)
                                                :context context
                                                :environment environment)
                                   (line/create line
                                                real-point-side real-point-top
                                                :real-start (-> ideal-point-side
                                                                (v/sub real-point-side)
                                                                v/abs)
                                                :real-end (-> ideal-point-top
                                                              (v/sub real-point-side)
                                                              v/abs)
                                                :context context
                                                :environment environment))
        part (if (= variant :dexter)
               [["M" (v/add real-point-top
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:left :top]
                                [real-point-side
                                 (v/add real-point-top
                                        line-one-start)])
                 "z"]
                [top-left ideal-point-side ideal-point-top]]
               [["M" (v/add real-point-side
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:top :right]
                                [real-point-top
                                 (v/add real-point-side
                                        line-one-start)])
                 "z"]
                [top-left ideal-point-side ideal-point-top]])]

    [:<>
     [field-shared/make-subfield
      (c/++ context :field)
      part
      :all]
     [line/render line [line-one-data] (case variant
                                         :dexter real-point-top
                                         :sinister real-point-side) outline? context]
     [cottising/render-bend-cottise
      (c/++ context :cottising :cottise-1)
      :cottise-2 :cottise-opposite-1
      :sinister? (= variant :dexter)
      :swap-lines? true
      :distance-fn (fn [distance thickness]
                     (-> (- distance)
                         (- (/ thickness 2))
                         (/ 100)
                         (* width)
                         (+ line-one-min)))
      :alignment :right
      :width width
      :height height
      :angle (if (= variant :dexter)
               (- (v/angle-to-point ideal-point-side ideal-point-top))
               (v/angle-to-point ideal-point-top ideal-point-side))
      :direction-orthogonal (cond-> (v/orthogonal line-dir-normed)
                              (= variant :sinister) (v/mul -1))
      :center-point (-> ideal-point-side
                        (v/add ideal-point-top)
                        (v/div 2))
      :middle-real-start-fn (fn [point-offset]
                              (v/sub (if (= variant :dexter)
                                       ideal-point-side
                                       ideal-point-top) point-offset))
      :middle-real-end-fn (fn [point-offset]
                            (v/sub (if (= variant :dexter)
                                     ideal-point-top
                                     ideal-point-side) point-offset))]]))
