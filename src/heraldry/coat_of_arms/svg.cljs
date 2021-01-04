(ns heraldry.coat-of-arms.svg
  (:require [clojure.string :as s]
            [heraldry.coat-of-arms.vector :as v]))

(defn new-path [d]
  (let [p (js/document.createElementNS "http://www.w3.org/2000/svg" "path")]
    (.setAttribute p "d" d)
    p))

(defn points [^js/SVGPath path n]
  (let [length (.getTotalLength path)]
    (mapv (fn [i]
            (let [p (.getPointAtLength path (-> length (* i) (/ n)))]
              (v/v (.-x p) (.-y p)))) (range (inc n)))))

(defn min-max-x-y [[{x :x y :y} & rest]]
  (reduce (fn [[min-x max-x min-y max-y] {x :x y :y}]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn avg-x-y [[p & rest]]
  (let [[s n] (reduce (fn [[s n] p]
                        [(v/+ s p)
                         (inc n)])
                      [p 1]
                      rest)]
    (v// s n)))

(defn bounding-box-from-path [d]
  (let [path (new-path d)
        points (points path 50)
        box (min-max-x-y points)]
    box))

(defn bounding-box [points]
  (min-max-x-y points))

(defn center [d]
  (let [path (new-path d)
        points (points path 50)
        center (avg-x-y points)]
    center))

(defn make-path [v]
  (cond
    (string? v) v
    (and (map? v)
         (:x v)
         (:y v)) (str (:x v) "," (:y v))
    (sequential? v) (s/join " " (map make-path v))
    :else (str v)))

(defn rotated-bounding-box [{x1 :x y1 :y :as p1} {x2 :x y2 :y :as p2} rotation & {:keys [middle scale]}]
  (let [middle (or middle
                   (v/avg p1 p2))
        scale (or scale
                  (v/v 1 1))
        points [(v/+ middle
                     (v/rotate (v/dot (v/- (v/v x1 y1)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x2 y1)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x1 y2)
                                           middle)
                                      scale) rotation))
                (v/+ middle
                     (v/rotate (v/dot (v/- (v/v x2 y2)
                                           middle)
                                      scale) rotation))]]
    (bounding-box points)))
