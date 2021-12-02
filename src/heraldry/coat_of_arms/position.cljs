(ns heraldry.coat-of-arms.position
  (:require
   [heraldry.gettext :refer [string]]
   [heraldry.math.vector :as v]
   [heraldry.util :as util]))

(def point-choices
  [[(string "Fess [point]") :fess]
   [(string "Chief [point]") :chief]
   [(string "Base [point]") :base]
   [(string "Dexter [point]") :dexter]
   [(string "Sinister [point]") :sinister]
   [(string "Honour [point]") :honour]
   [(string "Nombril [point]") :nombril]
   [(string "Top-left") :top-left]
   [(string "Top") :top]
   [(string "Top-right") :top-right]
   [(string "Left") :left]
   [(string "Right") :right]
   [(string "Bottom-left") :bottom-left]
   [(string "Bottom") :bottom]
   [(string "Bottom-right") :bottom-right]])

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
  [[(string "Top-left") :top-left]
   [(string "Top") :top]
   [(string "Top-right") :top-right]
   [(string "Left") :left]
   [(string "Right") :right]
   [(string "Bottom-left") :bottom-left]
   [(string "Bottom") :bottom]
   [(string "Bottom-right") :bottom-right]
   [(string "Fess [point]") :fess]
   [(string "Chief [point]") :chief]
   [(string "Base [point]") :base]
   [(string "Dexter [point]") :dexter]
   [(string "Sinister [point]") :sinister]
   [(string "Honour [point]") :honour]
   [(string "Nombril [point]") :nombril]
   [(string "Angle") :angle]])

(def anchor-point-map
  (util/choices->map anchor-point-choices))

(def alignment-choices
  [[(string "Left") :left]
   [(string "Middle") :middle]
   [(string "Right") :right]])

(def alignment-map
  (util/choices->map alignment-choices))

(def default-options
  {:point {:type :choice
           :choices point-choices
           :default :fess
           :ui {:label (string "Point")}}
   :alignment {:type :choice
               :choices alignment-choices
               :default :middle
               :ui {:label (string "Alignment")
                    :form-type :radio-select}}
   :angle {:type :range
           :min 0
           :max 360
           :default 0
           :ui {:label (string "Angle")}}
   :offset-x {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label (string "Offset x")
                   :step 0.1}}
   :offset-y {:type :range
              :min -45
              :max 45
              :default 0
              :ui {:label (string "Offset y")
                   :step 0.1}}
   :ui {:label (string "Position")
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
