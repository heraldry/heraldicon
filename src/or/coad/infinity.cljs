(ns or.coad.infinity)

(defn top [& [point]]
  (-> point
      (or {:x 0 :y 0})
      (assoc :y -1000)))

(defn bottom [& [point]]
  (-> point
      (or {:x 0 :y 0})
      (assoc :y 1000)))

(defn left [& [point]]
  (-> point
      (or {:x 0 :y 0})
      (assoc :x -1000)))

(defn right [& [point]]
  (-> point
      (or {:x 0 :y 0})
      (assoc :x 1000)))

(defn top-left [& _]
  {:x -1000
   :y -1000})

(defn top-right [& _]
  {:x 1000
   :y -1000})

(defn bottom-left [& _]
  {:x -1000
   :y 1000})

(defn bottom-right [& _]
  {:x 1000
   :y 1000})

(defn function [type]
  (get {:top          top
        :bottom       bottom
        :left         left
        :right        right
        :top-left     top-left
        :top-right    top-right
        :bottom-left  bottom-left
        :bottom-right bottom-right} type))

(def clockwise-points
  [:top-left :top :top-right :right :bottom-right :bottom :bottom-left :left])

(def counter-clockwise-points
  [:top-left :left :bottom-left :bottom :bottom-right :right :top-right :top])

(defn relevant-points [points from to]
  (let [points-before-from (vec (take-while #(not= % from) points))
        rest               (subvec points (count points-before-from))
        shifted-points     (vec (concat rest points-before-from))
        relevant-points    (conj (vec (take-while #(not= % to) shifted-points)) to)]
    relevant-points))

(defn path [direction [from to] [start end]]
  (let [points        (relevant-points (case direction
                                         :clockwise         clockwise-points
                                         :counter-clockwise counter-clockwise-points)
                                       from to)
        first-point   (first points)
        last-point    (last points)
        middle-points (->> points
                           (drop 1)
                           (drop-last)
                           vec)
        path          (->
                       ["L" ((function first-point) start)]
                       (into (map (fn [point]
                                    ((function point))) middle-points))
                       (into ["L" ((function last-point) end)
                              "L" end]))]
    path))
