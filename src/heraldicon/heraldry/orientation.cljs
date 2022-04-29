(ns heraldicon.heraldry.orientation
  (:require
   [clojure.set :as set]
   [heraldicon.heraldry.position :as position]
   [heraldicon.math.vector :as v]))

(defn calculate-anchor-and-orientation [environment anchor orientation width base-angle]
  (let [target-anchor (position/calculate anchor environment)
        target-orientation (position/calculate-orientation orientation environment target-anchor
                                                           (or base-angle 0))
        ;; TODO: this is a hack to avoid both points being the same, which causes errors,
        ;; but it might not always be the right thing to do, so far I've only seen it for
        ;; very special per-chevron partitions
        target-orientation (cond-> target-orientation
                             (= target-orientation
                                target-anchor) (update :y - 10))
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
