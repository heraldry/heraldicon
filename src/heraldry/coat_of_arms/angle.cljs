(ns heraldry.coat-of-arms.angle
  (:require [clojure.set :as set]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.vector :as v]))

(defn calculate-origin-and-anchor [environment origin anchor width base-angle]
  (let [target-origin (position/calculate origin environment)
        target-anchor (position/calculate-anchor anchor environment target-origin
                                                 (or base-angle 0))
        origin-align (or (:alignment origin) :middle)
        anchor-align (if (-> anchor :point (= :angle))
                       origin-align
                       (or (:alignment anchor) :middle))
        r (/ width 2)
        alignments (set [origin-align anchor-align])
        outer-tangent? (or (set/subset? alignments #{:middle :left})
                           (set/subset? alignments #{:middle :right}))
        [real-origin real-anchor] (if outer-tangent?
                                    (v/outer-tangent-between-circles target-origin (case origin-align
                                                                                     :middle 0
                                                                                     r)
                                                                     target-anchor (case anchor-align
                                                                                     :middle 0
                                                                                     r)
                                                                     (or (:left alignments)
                                                                         (:right alignments)))
                                    (v/inner-tangent-between-circles target-origin (case origin-align
                                                                                     :middle 0
                                                                                     r)
                                                                     target-anchor (case anchor-align
                                                                                     :middle 0
                                                                                     r)
                                                                     anchor-align))]
    {:real-origin real-origin
     :real-anchor real-anchor}))
