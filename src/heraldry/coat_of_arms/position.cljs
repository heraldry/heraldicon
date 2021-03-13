(ns heraldry.coat-of-arms.position
  (:require [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(def point-choices
  [["Fess" :fess]
   ["Chief" :chief]
   ["Base" :base]
   ["Dexter" :dexter]
   ["Sinister" :sinister]
   ["Honour" :honour]
   ["Nombril" :nombril]])

(def point-choices-x
  (->> point-choices
       (filter (fn [[_ k]]
                 (#{:dexter :fess :sinister} k)))
       vec))

(def point-choices-y
  (util/filter-choices
   point-choices
   #{:chief :honour :fess :nombril :base}))

(def point-map
  (util/choices->map point-choices))

(def anchor-point-choices
  [["Top-left" :top-left]
   ["Top-right" :top-right]
   ["Top" :top]
   ["Left" :left]
   ["Right" :right]
   ["Bottom-left" :bottom-left]
   ["Bottom-right" :bottom-right]
   ["Bottom" :bottom]
   ["Fess" :fess]
   ["Chief" :chief]
   ["Base" :base]
   ["Dexter" :dexter]
   ["Sinister" :sinister]
   ["Honour" :honour]
   ["Nombril" :nombril]
   ["Fixed angle" :angle]])

(def anchor-point-map
  (util/choices->map anchor-point-choices))

(def alignment-choices
  [["Middle" :middle]
   ["Left edge" :left]
   ["Right edge" :right]])

(def alignment-map
  (util/choices->map alignment-choices))

(def default-options
  {:point {:type :choice
           :choices point-choices
           :default :fess}
   :alignment {:type :choice
               :choices alignment-choices
               :default :middle}
   :offset-x {:type :range
              :min -45
              :max 45
              :default 0}
   :offset-y {:type :range
              :min -45
              :max 45
              :default 0}})

(def anchor-default-options
  (-> default-options
      (assoc-in [:point :choices] anchor-point-choices)
      (assoc :angle {:type :range
                     :min 10
                     :max 80
                     :default 45})))

(defn adjust-options [options values]
  (cond-> options
    (-> values :point (not= :angle)) (dissoc :angle)
    (-> values :point (= :angle)) (->
                                   (dissoc :offset-x)
                                   (dissoc :offset-y)
                                   (dissoc :alignment))))

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
      (v/+ origin (v/* (v/v (Math/cos angle-rad)
                            (Math/sin angle-rad))
                       200)))
    (calculate anchor environment)))
