(ns heraldry.coat-of-arms.field.type.tierced-per-pale
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.math.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.math.svg :as svg]))

(def field-type :heraldry.field.type/tierced-per-pale)

(defmethod field-interface/display-name field-type [_] "Tierced per pale")

(defmethod field-interface/part-names field-type [_] ["dexter" "fess" "sinister"])

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        stretch-x (interface/get-sanitized-data (conj path :layout :stretch-x) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top (assoc (:top points) :x (:x origin-point))
        top-left (:top-left points)
        bottom (assoc (:bottom points) :x (:x origin-point))
        bottom-right (:bottom-right points)
        width (:width environment)
        middle-half-width (-> width
                              (/ 6)
                              (* stretch-x))
        col1 (- (:x origin-point) middle-half-width)
        col2 (+ (:x origin-point) middle-half-width)
        [first-top first-bottom] (v/environment-intersections
                                  (v/v col1 (:y top))
                                  (v/v col1 (:y bottom))
                                  environment)
        [second-top second-bottom] (v/environment-intersections
                                    (v/v col2 (:y top))
                                    (v/v col2 (:y bottom))
                                    environment)
        shared-start-y (- (min (:y first-top)
                               (:y second-top))
                          30)
        real-start (min (-> first-top :y (- shared-start-y))
                        (-> second-top :y (- shared-start-y)))
        real-end (max (-> first-bottom :y (- shared-start-y))
                      (-> second-bottom :y (- shared-start-y)))
        shared-end-y (+ real-end 30)
        first-top (v/v (:x first-top) shared-start-y)
        second-top (v/v (:x second-top) shared-start-y)
        first-bottom (v/v (:x first-bottom) shared-end-y)
        second-bottom (v/v (:x second-bottom) shared-end-y)
        {line-one :line
         line-one-start :line-start} (line/create line
                                                  first-top first-bottom
                                                  :real-start real-start
                                                  :real-end real-end
                                                  :context context
                                                  :environment environment)
        {line-reversed :line
         line-reversed-start :line-start} (line/create line
                                                       second-top second-bottom
                                                       :reversed? true
                                                       :flipped? true
                                                       :mirrored? true
                                                       :real-start real-start
                                                       :real-end real-end
                                                       :context context
                                                       :environment environment)
        parts [[["M" (v/add first-top
                            line-one-start)
                 (svg/stitch line-one)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [(v/add first-bottom
                                        line-one-start)
                                 (v/add first-top
                                        line-one-start)])
                 "z"]
                [top-left
                 first-bottom]]

               [["M" (v/add second-bottom
                            line-reversed-start)
                 (svg/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:top :top]
                                [(v/add second-top
                                        line-reversed-start)
                                 (v/add first-top
                                        line-one-start)])
                 (svg/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:bottom :bottom]
                                [(v/add first-top
                                        line-one-start)
                                 (v/add second-bottom
                                        line-reversed-start)
                                 first-bottom second-bottom])
                 "z"]
                [first-top
                 second-bottom]]

               [["M" (v/add second-bottom
                            line-reversed-start)
                 (svg/stitch line-reversed)
                 (infinity/path :clockwise
                                [:top :bottom]
                                [(v/add second-top
                                        line-reversed-start)
                                 (v/add second-bottom
                                        line-reversed-start)])
                 "z"]
                [second-top
                 bottom-right]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all
       [(svg/make-path
         ["M" (v/add second-bottom
                     line-reversed-start)
          (svg/stitch line-reversed)])]
       nil]
      environment context]
     (when outline?
       [:g (outline/style context)
        [:path {:d (svg/make-path
                    ["M" (v/add first-top
                                line-one-start)
                     (svg/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/add second-bottom
                                line-reversed-start)
                     (svg/stitch line-reversed)])}]])]))
