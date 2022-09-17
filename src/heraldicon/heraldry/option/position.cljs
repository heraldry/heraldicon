(ns heraldicon.heraldry.option.position
  (:require
   [clojure.set :as set]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(def ^:private all-anchor-choices
  [[:string.option.point-choice-group/heraldic
    [:string.option.point-choice/fess :fess]
    [:string.option.point-choice/chief :chief]
    [:string.option.point-choice/base :base]
    [:string.option.point-choice/dexter :dexter]
    [:string.option.point-choice/sinister :sinister]
    [:string.option.point-choice/honour :honour]
    [:string.option.point-choice/nombril :nombril]]
   [:string.option.point-choice-group/vexillological
    [:string.option.point-choice/hoist :hoist]
    [:string.option.point-choice/fly :fly]]
   [:string.option.point-choice-group/technical
    [:string.option.point-choice/auto :auto]
    [:string.option.point-choice/top-left :top-left]
    [:string.option.point-choice/top :top]
    [:string.option.point-choice/top-right :top-right]
    [:string.option.point-choice/left :left]
    [:string.option.point-choice/center :center]
    [:string.option.point-choice/right :right]
    [:string.option.point-choice/bottom-left :bottom-left]
    [:string.option.point-choice/bottom :bottom]
    [:string.option.point-choice/bottom-right :bottom-right]]])

(def ^:private all-orientation-choices
  [[:string.option.orientation-point-choice/angle :angle]
   [:string.option.point-choice-group/technical
    [:string.option.point-choice/auto :auto]
    [:string.option.point-choice/top-left :top-left]
    [:string.option.point-choice/top :top]
    [:string.option.point-choice/top-right :top-right]
    [:string.option.point-choice/left :left]
    [:string.option.point-choice/center :center]
    [:string.option.point-choice/right :right]
    [:string.option.point-choice/bottom-left :bottom-left]
    [:string.option.point-choice/bottom :bottom]
    [:string.option.point-choice/bottom-right :bottom-right]]
   [:string.option.point-choice-group/heraldic
    [:string.option.point-choice/fess :fess]
    [:string.option.point-choice/chief :chief]
    [:string.option.point-choice/base :base]
    [:string.option.point-choice/dexter :dexter]
    [:string.option.point-choice/sinister :sinister]
    [:string.option.point-choice/honour :honour]
    [:string.option.point-choice/nombril :nombril]]
   [:string.option.point-choice-group/vexillological
    [:string.option.point-choice/hoist :hoist]
    [:string.option.point-choice/fly :fly]]])

(defn anchor-choices [choices]
  (options/select-choices all-anchor-choices choices))

(defn orientation-choices [choices]
  (options/select-choices all-orientation-choices choices))

(def orientation-point-map
  (options/choices->map all-orientation-choices))

(def alignment-choices
  [[:string.option.alignment-choice/left :left]
   [:string.option.alignment-choice/middle :middle]
   [:string.option.alignment-choice/right :right]])

(def alignment-map
  (options/choices->map alignment-choices))

(defn calculate [{:keys [point offset-x offset-y] :or {offset-x 0
                                                       offset-y 0}} environment & [default]]
  (let [ref (or point default)
        ref (if (= ref :auto)
              :fess
              ref)
        p (-> environment :points (get ref))
        width (:width environment)
        height (:height environment)
        dx (-> offset-x
               (* width)
               (/ 100))
        dy (-> offset-y
               (* height)
               (/ 100)
               -)]
    (v/Vector. (-> p
                   :x
                   (+ dx))
               (-> p
                   :y
                   (+ dy)))))

(defn- calculate-orientation [{:keys [point angle] :as orientation} environment anchor base-angle]
  (if (= point :angle)
    (let [angle-rad (-> angle
                        (+ base-angle)
                        (* Math/PI) (/ 180))]
      (v/add anchor (v/mul (v/Vector. (Math/cos angle-rad)
                                      (Math/sin angle-rad))
                           200)))
    (calculate orientation environment)))

(defn calculate-anchor-and-orientation [environment anchor orientation width base-angle]
  (let [target-anchor (calculate anchor environment)
        target-orientation (calculate-orientation orientation environment target-anchor
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
