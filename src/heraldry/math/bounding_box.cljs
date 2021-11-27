(ns heraldry.math.bounding-box
  (:require
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]))

(defn min-max-x-y [[{x :x y :y} & rest]]
  (reduce (fn [[min-x max-x min-y max-y] {x :x y :y}]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn bounding-box-from-paths [paths]
  (let [points (mapcat
                #(-> %
                     path/parse-path
                     (path/points 50))
                paths)
        box (min-max-x-y points)]
    box))

(defn bounding-box [points]
  (min-max-x-y points))

(defn combine [[[first-min-x first-max-x
                 first-min-y first-max-y] & rest]]
  (reduce (fn [[min-x max-x min-y max-y]
               [next-min-x next-max-x next-min-y next-max-y]]
            [(min min-x next-min-x)
             (max max-x next-max-x)
             (min min-y next-min-y)
             (max max-y next-max-y)])
          [first-min-x first-max-x
           first-min-y first-max-y]
          rest))

(defn rotate [{x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2} rotation & {:keys [middle scale]}]
  (let [middle (or middle
                   (v/avg p1 p2))
        scale (or scale
                  (v/v 1 1))
        points [(v/add middle
                       (v/rotate (v/dot (v/sub (v/v x1 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x2 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x1 y2)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/v x2 y2)
                                               middle)
                                        scale) rotation))]]
    (bounding-box points)))
