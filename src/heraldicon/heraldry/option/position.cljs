(ns heraldicon.heraldry.option.position
  (:require
   [clojure.set :as set]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]))

(def point-choices
  [[:string.option.point-choice/fess :fess]
   [:string.option.point-choice/chief :chief]
   [:string.option.point-choice/base :base]
   [:string.option.point-choice/dexter :dexter]
   [:string.option.point-choice/sinister :sinister]
   [:string.option.point-choice/honour :honour]
   [:string.option.point-choice/nombril :nombril]
   [:string.option.point-choice/top-left :top-left]
   [:string.option.point-choice/top :top]
   [:string.option.point-choice/top-right :top-right]
   [:string.option.point-choice/left :left]
   [:string.option.point-choice/center :center]
   [:string.option.point-choice/right :right]
   [:string.option.point-choice/bottom-left :bottom-left]
   [:string.option.point-choice/bottom :bottom]
   [:string.option.point-choice/bottom-right :bottom-right]])

(def point-choices-x
  (->> point-choices
       (filter (fn [[_ k]]
                 (#{:dexter :fess :sinister} k)))
       vec))

(def point-choices-y
  (options/filter-choices
   point-choices
   #{:chief :honour :fess :nombril :base}))

(def orientation-point-choices
  [[:string.option.point-choice/top-left :top-left]
   [:string.option.point-choice/top :top]
   [:string.option.point-choice/top-right :top-right]
   [:string.option.point-choice/left :left]
   [:string.option.point-choice/center :center]
   [:string.option.point-choice/right :right]
   [:string.option.point-choice/bottom-left :bottom-left]
   [:string.option.point-choice/bottom :bottom]
   [:string.option.point-choice/bottom-right :bottom-right]
   [:string.option.point-choice/fess :fess]
   [:string.option.point-choice/chief :chief]
   [:string.option.point-choice/base :base]
   [:string.option.point-choice/dexter :dexter]
   [:string.option.point-choice/sinister :sinister]
   [:string.option.point-choice/honour :honour]
   [:string.option.point-choice/nombril :nombril]
   [:string.option.orientation-point-choice/angle :angle]])

(def orientation-point-map
  (options/choices->map orientation-point-choices))

(def alignment-choices
  [[:string.option.alignment-choice/left :left]
   [:string.option.alignment-choice/middle :middle]
   [:string.option.alignment-choice/right :right]])

(def alignment-map
  (options/choices->map alignment-choices))

(def default-options
  {:point {:type :choice
           :choices point-choices
           :default :fess
           :ui {:label :string.option/point}}
   :alignment {:type :choice
               :choices alignment-choices
               :default :middle
               :ui {:label :string.option/alignment
                    :form-type :radio-select}}
   :angle {:type :range
           :min 0
           :max 360
           :default 0
           :ui {:label :string.option/angle}}
   :offset-x {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label :string.option/offset-x
                   :step 0.1}}
   :offset-y {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label :string.option/offset-y
                   :step 0.1}}
   :ui {:label :string.option/position
        :form-type :position}})

(def orientation-default-options
  (-> default-options
      (assoc-in [:point :choices] orientation-point-choices)
      (assoc :angle (merge (:angle default-options)
                           {:min 10
                            :max 80
                            :default 45}))))

(defn calculate [{:keys [point offset-x offset-y] :or {offset-x 0
                                                       offset-y 0}} environment & [default]]
  (let [ref (or point default)
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

(defn calculate-orientation [{:keys [point angle] :as orientation} environment anchor base-angle]
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
