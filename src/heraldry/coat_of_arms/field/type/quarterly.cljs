(ns heraldry.coat-of-arms.field.type.quarterly
  (:require
   [heraldry.coat-of-arms.field.interface :as field-interface]
   [heraldry.coat-of-arms.field.shared :as shared]
   [heraldry.coat-of-arms.infinity :as infinity]
   [heraldry.coat-of-arms.outline :as outline]
   [heraldry.context :as c]
   [heraldry.interface :as interface]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]))

(def field-type :heraldry.field.type/quarterly)

(defmethod field-interface/display-name field-type [_] {:en "Quarterly NxM"
                                                        :de "Geviert NxM"})

(defmethod field-interface/part-names field-type [_] nil)

(defmethod field-interface/render-field field-type
  [{:keys [path environment] :as context}]
  (let [num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (:points environment)
        top-left (:top-left points)
        bottom-right (:bottom-right points)
        width (- (:x bottom-right)
                 (:x top-left))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        required-width (* part-width
                          num-fields-x)
        middle-x (-> width
                     (/ 2)
                     (+ (:x top-left)))
        x0 (-> middle-x
               (- (/ required-width 2))
               (+ (* offset-x
                     part-width)))
        height (- (:y bottom-right)
                  (:y top-left))
        part-height (-> height
                        (/ num-fields-y)
                        (* stretch-y))
        required-height (* part-height
                           num-fields-y)
        middle-y (-> height
                     (/ 2)
                     (+ (:y top-left)))
        y0 (-> middle-y
               (- (/ required-height 2))
               (+ (* offset-y
                     part-height)))
        parts (->> (for [j (range num-fields-y)
                         i (range num-fields-x)]
                     (let [x1 (+ x0 (* i part-width))
                           x2 (+ x1 part-width)
                           y1 (+ y0 (* j part-height))
                           y2 (+ y1 part-height)
                           first-x? (zero? i)
                           first-y? (zero? j)
                           last-x? (-> num-fields-x dec (= i))
                           last-y? (-> num-fields-y dec (= j))]
                       (cond
                         (and first-x?
                              last-x?
                              first-y?
                              last-y?) [["M" -1000 -1000
                                         "h" 2000
                                         "v" 2000
                                         "h" -2000
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         (and first-x?
                              first-y?) [["M" [x1 y2]
                                          "L" [x2 y2]
                                          "L" [x2 y1]
                                          (infinity/path :counter-clockwise
                                                         [:top :left]
                                                         [(v/v x2 y1) (v/v x1 y2)])
                                          "z"]
                                         [(v/v x1 y1) (v/v x2 y2)]]
                         (and last-x?
                              first-y?) [["M" [x1 y1]
                                          "L" [x1 y2]
                                          "L" [x2 y2]
                                          (infinity/path :counter-clockwise
                                                         [:right :top]
                                                         [(v/v x2 y2) (v/v x1 y1)])
                                          "z"]
                                         [(v/v x1 y1) (v/v x2 y2)]]
                         (and first-x?
                              last-y?) [["M" [x1 y1]
                                         "L" [x2 y1]
                                         "L" [x2 y2]
                                         (infinity/path :clockwise
                                                        [:bottom :left]
                                                        [(v/v x2 y2) (v/v x1 y1)])
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         (and last-x?
                              last-y?) [["M" [x1 y2]
                                         "L" [x1 y1]
                                         "L" [x2 y1]
                                         (infinity/path :clockwise
                                                        [:right :bottom]
                                                        [(v/v x2 y1) (v/v x1 y2)])
                                         "z"]
                                        [(v/v x1 y1) (v/v x2 y2)]]
                         first-x? [["M" [x1 y1]
                                    "L" [x2 y1]
                                    "L" [x2 y2]
                                    "L" [x1 y2]
                                    (infinity/path :counter-clockwise
                                                   [:left :left]
                                                   [(v/v x1 y2) (v/v x1 y1)])
                                    "z"]
                                   [(v/v x1 y1) (v/v x2 y2)]]
                         first-y? [["M" [x1 y1]
                                    "L" [x1 y2]
                                    "L" [x2 y2]
                                    "L" [x2 y1]
                                    (infinity/path :counter-clockwise
                                                   [:top :top]
                                                   [(v/v x2 y1) (v/v x1 y1)])
                                    "z"]
                                   [(v/v x1 y1) (v/v x2 y2)]]
                         last-x? [["M" [x2 y2]
                                   "L" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   (infinity/path :clockwise
                                                  [:right :right]
                                                  [(v/v x2 y1) (v/v x2 y2)])
                                   "z"]
                                  [(v/v x1 y1) (v/v x2 y2)]]
                         last-y? [["M" [x1 y2]
                                   "L" [x1 y1]
                                   "L" [x2 y1]
                                   "L" [x2 y2]
                                   (infinity/path :clockwise
                                                  [:bottom :bottom]
                                                  [(v/v x2 y2) (v/v x1 y2)])
                                   "z"]
                                  [(v/v x1 y1) (v/v x2 y2)]]
                         :else [["M" [x1 y1]
                                 "L" [x2 y1]
                                 "L" [x2 y2]
                                 "L" [x1 y2]
                                 "z"]
                                [(v/v x1 y1) (v/v x2 y2)]])))
                   vec)
        overlap (->> (for [j (range num-fields-y)
                           i (range num-fields-x)]
                       (let [x1 (+ x0 (* i part-width))
                             x2 (+ x1 part-width)
                             y1 (+ y0 (* j part-height))
                             y2 (+ y1 part-height)]
                         [(path/make-path ["M" [x2 y1]
                                           "L" [x2 y2]
                                           "L" [x1 y2]])]))
                     vec)
        outline-extra 50
        outlines (when outline?
                   [:g (outline/style context)
                    (for [i (range 1 num-fields-x)]
                      (let [x1 (+ x0 (* i part-width))]
                        ^{:key [:x i]}
                        [:path {:d (path/make-path ["M" [x1 (- y0 outline-extra)]
                                                    "L" [x1 (+ y0 required-height outline-extra)]])}]))
                    (for [j (range 1 num-fields-y)]
                      (let [y1 (+ y0 (* j part-height))]
                        ^{:key [:y j]}
                        [:path {:d (path/make-path ["M" [(- x0 outline-extra) y1]
                                                    "L" [(+ x0 required-width outline-extra) y1]])}]))])]
    [:<>
     [shared/make-subfields
      path parts
      overlap
      environment context]
     outlines]))
