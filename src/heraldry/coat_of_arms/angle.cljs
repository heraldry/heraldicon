(ns heraldry.coat-of-arms.angle
  (:require
   [clojure.set :as set]
   [heraldry.coat-of-arms.position :as position]
   [heraldry.math.vector :as v]))

(defn calculate-origin-and-orientation [environment origin orientation width base-angle]
  (let [target-origin (position/calculate origin environment)
        target-orientation (position/calculate-orientation orientation environment target-origin
                                                           (or base-angle 0))
        origin-align (or (:alignment origin) :middle)
        orientation-align (if (-> orientation :point (= :angle))
                            origin-align
                            (or (:alignment orientation) :middle))
        r (/ width 2)
        alignments (set [origin-align orientation-align])
        outer-tangent? (or (set/subset? alignments #{:middle :left})
                           (set/subset? alignments #{:middle :right}))
        [real-origin real-orientation] (if outer-tangent?
                                         (v/outer-tangent-between-circles target-origin (case origin-align
                                                                                          :middle 0
                                                                                          r)
                                                                          target-orientation (case orientation-align
                                                                                               :middle 0
                                                                                               r)
                                                                          (or (:left alignments)
                                                                              (:right alignments)))
                                         (v/inner-tangent-between-circles target-origin (case origin-align
                                                                                          :middle 0
                                                                                          r)
                                                                          target-orientation (case orientation-align
                                                                                               :middle 0
                                                                                               r)
                                                                          orientation-align))]
    {:real-origin real-origin
     :real-orientation real-orientation}))
