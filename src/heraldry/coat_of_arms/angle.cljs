(ns heraldry.coat-of-arms.angle
  (:require
   [clojure.set :as set]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.math.vector :as v]))

(defn calculate-anchor-and-orientation [environment anchor orientation width base-angle]
  (let [target-anchor (position/calculate anchor environment)
        target-orientation (position/calculate-orientation orientation environment target-anchor
                                                           (or base-angle 0))
        anchor-align (or (:alignment anchor) :middle)
        orientation-align (if (-> orientation :point (= :angle))
                            anchor-align
                            (or (:alignment orientation) :middle))
        r (/ width 2)
        alignments (set [anchor-align orientation-align])
        outer-tangent? (or (set/subset? alignments #{:middle :left})
                           (set/subset? alignments #{:middle :right}))
        [real-anchor real-orientation] (if outer-tangent?
                                         (v/outer-tangent-between-circles target-anchor (case anchor-align
                                                                                          :middle 0
                                                                                          r)
                                                                          target-orientation (case orientation-align
                                                                                               :middle 0
                                                                                               r)
                                                                          (or (:left alignments)
                                                                              (:right alignments)))
                                         (v/inner-tangent-between-circles target-anchor (case anchor-align
                                                                                          :middle 0
                                                                                          r)
                                                                          target-orientation (case orientation-align
                                                                                               :middle 0
                                                                                               r)
                                                                          orientation-align))]
    {:real-anchor real-anchor
     :real-orientation real-orientation}))
