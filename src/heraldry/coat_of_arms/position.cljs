(ns heraldry.coat-of-arms.position
  (:require
   [heraldry.math.vector :as v]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(def point-choices
  [[strings/fess-point :fess]
   [strings/chief-point :chief]
   [strings/base-point :base]
   [strings/dexter-point :dexter]
   [strings/sinister-point :sinister]
   [strings/honour-point :honour]
   [strings/nombril-point :nombril]
   [strings/top-left :top-left]
   [strings/top :top]
   [strings/top-right :top-right]
   [strings/left :left]
   [strings/right :right]
   [strings/bottom-left :bottom-left]
   [strings/bottom :bottom]
   [strings/bottom-right :bottom-right]])

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
  [[strings/top-left :top-left]
   [strings/top :top]
   [strings/top-right :top-right]
   [strings/left :left]
   [strings/right :right]
   [strings/bottom-left :bottom-left]
   [strings/bottom :bottom]
   [strings/bottom-right :bottom-right]
   [strings/fess-point :fess]
   [strings/chief-point :chief]
   [strings/base-point :base]
   [strings/dexter-point :dexter]
   [strings/sinister-point :sinister]
   [strings/honour-point :honour]
   [strings/nombril-point :nombril]
   [strings/angle :angle]])

(def anchor-point-map
  (util/choices->map anchor-point-choices))

(def alignment-choices
  [[strings/left :left]
   [strings/middle :middle]
   [strings/right :right]])

(def alignment-map
  (util/choices->map alignment-choices))

(def default-options
  {:point {:type :choice
           :choices point-choices
           :default :fess
           :ui {:label strings/point}}
   :alignment {:type :choice
               :choices alignment-choices
               :default :middle
               :ui {:label strings/alignment
                    :form-type :radio-select}}
   :angle {:type :range
           :min 0
           :max 360
           :default 0
           :ui {:label strings/angle}}
   :offset-x {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label strings/offset-x
                   :step 0.1}}
   :offset-y {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label strings/offset-y
                   :step 0.1}}
   :ui {:label strings/position
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
