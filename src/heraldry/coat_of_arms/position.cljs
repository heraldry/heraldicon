(ns heraldry.coat-of-arms.position
  (:require
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

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
  (util/filter-choices
   point-choices
   #{:chief :honour :fess :nombril :base}))

(def anchor-point-choices
  [[:string.option.point-choice/top-left :top-left]
   [:string.option.point-choice/top :top]
   [:string.option.point-choice/top-right :top-right]
   [:string.option.point-choice/left :left]
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
   [:string.option.anchor-point-choice/angle :angle]])

(def anchor-point-map
  (util/choices->map anchor-point-choices))

(def alignment-choices
  [[:string.option.alignment-choice/left :left]
   [:string.option.alignment-choice/middle :middle]
   [:string.option.alignment-choice/right :right]])

(def alignment-map
  (util/choices->map alignment-choices))

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

(def anchor-default-options
  (-> default-options
      (assoc-in [:point :choices] anchor-point-choices)
      (assoc :angle (merge (:angle default-options)
                           {:min 10
                            :max 80
                            :default 45}))))

(defn calculate [{:keys [point offset-x offset-y] :or {offset-x 0
                                                       offset-y 0}} environment & [default]]
  (let [ref (-> point
                (or default))
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
    (v/v (-> p
             :x
             (+ dx))
         (-> p
             :y
             (+ dy)))))

(defn calculate-anchor [{:keys [point angle] :as anchor} environment origin base-angle]
  (if (= point :angle)
    (let [angle-rad (-> angle
                        (+ base-angle)
                        (* Math/PI) (/ 180))]
      (v/add origin (v/mul (v/v (Math/cos angle-rad)
                                (Math/sin angle-rad))
                           200)))
    (calculate anchor environment)))
