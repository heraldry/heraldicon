(ns heraldry.coat-of-arms.field.type.tierced-per-fess
  (:require [heraldry.coat-of-arms.field.interface :as field-interface]
            [heraldry.coat-of-arms.field.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.math.vector :as v]
            [heraldry.interface :as interface]
            [heraldry.math.svg.path :as path]
            [heraldry.math.svg.core :as svg]))

(def field-type :heraldry.field.type/tierced-per-fess)

(defmethod field-interface/display-name field-type [_] "Tierced per fess")

(defmethod field-interface/part-names field-type [_] ["chief" "fess" "base"])

(defmethod field-interface/render-field field-type
  [path environment context]
  (let [line (interface/get-sanitized-data (conj path :line) context)
        stretch-y (interface/get-sanitized-data (conj path :layout :stretch-y) context)
        origin (interface/get-sanitized-data (conj path :origin) context)
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (conj path :outline?) context))
        points (:points environment)
        origin-point (position/calculate origin environment :fess)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        left (assoc (:left points) :y (:y origin-point))
        right (assoc (:right points) :y (:y origin-point))
        height (:height environment)
        middle-half-height (-> height
                               (/ 6)
                               (* stretch-y))
        row1 (- (:y origin-point) middle-half-height)
        row2 (+ (:y origin-point) middle-half-height)
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
        {line-one :line
         line-one-start :line-start} (line/create line
                                                  first-left first-right
                                                  :real-start real-start
                                                  :real-end real-end
                                                  :context context
                                                  :environment environment)
        {line-reversed :line
         line-reversed-start :line-start} (line/create line
                                                       second-left second-right
                                                       :reversed? true
                                                       :flipped? true
                                                       :mirrored? true
                                                       :real-start real-start
                                                       :real-end real-end
                                                       :context context
                                                       :environment environment)
        parts [[["M" (v/add first-left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [(v/add first-right
                                        line-one-start)
                                 (v/add first-left
                                        line-one-start)])
                 "z"]
                [top-left first-right]]

               [["M" (v/add first-left
                            line-one-start)
                 (path/stitch line-one)
                 (infinity/path :clockwise
                                [:right :right]
                                [(v/add first-left
                                        line-one-start)
                                 (v/add second-right
                                        line-reversed-start)])
                 (path/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :left]
                                [(v/add second-left
                                        line-reversed-start)
                                 (v/add first-left
                                        line-one-start)])
                 "z"]
                [first-left
                 second-right]]

               [["M" (v/add second-right
                            line-reversed-start)
                 (path/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [(v/add second-left
                                        line-reversed-start)
                                 (v/add second-right
                                        line-reversed-start)])
                 "z"]
                [second-left bottom-right]]]]
    [:<>
     [shared/make-subfields
      path parts
      [:all
       [(path/make-path
         ["M" (v/add second-right
                     line-reversed-start)
          (path/stitch line-reversed)])]
       nil]
      environment context]
     (when outline?
       [:g (outline/style context)
        [:path {:d (path/make-path
                    ["M" (v/add first-left
                                line-one-start)
                     (path/stitch line-one)])}]
        [:path {:d (path/make-path
                    ["M" (v/add second-right
                                line-reversed-start)
                     (path/stitch line-reversed)])}]])]))
