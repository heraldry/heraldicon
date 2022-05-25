(ns heraldicon.svg.infinity
  (:require
   [heraldicon.math.vector :as v]))

(defn top [& [point]]
  (-> point
      (or v/zero)
      (assoc :y -1000)))

(defn bottom [& [point]]
  (-> point
      (or v/zero)
      (assoc :y 1000)))

(defn left [& [point]]
  (-> point
      (or v/zero)
      (assoc :x -1000)))

(defn right [& [point]]
  (-> point
      (or v/zero)
      (assoc :x 1000)))

(defn top-left [& _]
  (v/Vector. -1000 -1000))

(defn top-right [& _]
  (v/Vector. 1000 -1000))

(defn bottom-left [& _]
  (v/Vector. -1000 1000))

(defn bottom-right [& _]
  (v/Vector. 1000 1000))

(defn function [type]
  (get {:top top
        :bottom bottom
        :left left
        :right right
        :top-left top-left
        :top-right top-right
        :bottom-left bottom-left
        :bottom-right bottom-right} type))

(def clockwise-points
  [:top-left :top :top-right :right :bottom-right :bottom :bottom-left :left])

(def counter-clockwise-points
  [:top-left :left :bottom-left :bottom :bottom-right :right :top-right :top])

(defn relevant-points [points from to]
  (let [points-before-from (vec (take-while #(not= % from) points))
        rest (subvec points (count points-before-from))
        shifted-points (vec (concat rest points-before-from))
        relevant-points (conj (vec (take-while #(not= % to) shifted-points)) to)]
    relevant-points))

(defn path [direction [from to] [start end]]
  (let [points (relevant-points (case direction
                                  :clockwise clockwise-points
                                  :counter-clockwise counter-clockwise-points)
                                from to)
        first-point (first points)
        last-point (last points)
        middle-points (->> points
                           (drop 1)
                           (drop-last)
                           vec)
        path (-> ["L" ((function first-point) start)]
                 (into (map (fn [point] ((function point))))
                       middle-points)
                 (into ["L" ((function last-point) end)
                        "L" end]))]
    path))
