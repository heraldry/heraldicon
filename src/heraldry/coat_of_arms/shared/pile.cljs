(ns heraldry.coat-of-arms.shared.pile
  (:require [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn diagonals [origin-point point-point size]
  (let [direction-vector (v/- point-point origin-point)
        direction-length (v/abs direction-vector)
        direction (v// direction-vector direction-length)
        direction-orthogonal (v/orthogonal direction)
        left-point (v/+ origin-point
                        (v/* direction-orthogonal (/ size 2)))
        right-point (v/- origin-point
                         (v/* direction-orthogonal (/ size 2)))]
    {:left (-> left-point
               (v/- point-point)
               v/normal
               (v/* 500)
               (v/+ point-point))
     :right (-> right-point
                (v/- point-point)
                v/normal
                (v/* 500)
                (v/+ point-point))}))

(defn calculate-angle [target-beta edge-length stretch]
  (let [tan-beta (-> target-beta
                     (* Math/PI)
                     (/ 180)
                     Math/tan)
        dx (* stretch tan-beta)
        f (/ edge-length
             (-> (* dx dx)
                 (+ 1)
                 Math/sqrt))
        rx (* f dx)]
    {:angle (-> rx
                (/ edge-length)
                Math/asin
                (* 180)
                (/ Math/PI))
     :length f
     :part-length (* f stretch)
     :x rx}))

(defn calculate-properties-for-angle [environment origin anchor
                                      {:keys [size stretch]}
                                      _thickness-base base-angle]
  (let [anchor-type (or (:type anchor)
                        :edge)
        alignment (-> origin :alignment (or :middle))
        {real-origin :real-origin
         real-anchor :real-anchor} (angle/calculate-origin-and-anchor
                                    environment
                                    (assoc origin :alignment :middle)
                                    (assoc anchor :alignment :middle)
                                    0
                                    base-angle)
        target-point (case anchor-type
                       :edge (v/find-intersection
                              real-origin real-anchor
                              environment)
                       real-anchor)
        direction-vector (v/- target-point real-origin)
        direction-length (v/abs direction-vector)
        {:keys [angle x
                length
                part-length]} (case alignment
                                :middle {:angle 0
                                         :length direction-length
                                         :part-length (* direction-length stretch)
                                         :x (-> size
                                                (/ 2)
                                                (* Math/PI)
                                                (/ 180)
                                                Math/tan
                                                (* direction-length stretch))}
                                (calculate-angle (/ size 2) direction-length stretch))
        angle (case alignment
                :left (- angle)
                angle)
        calculated-origin (-> real-origin
                              (v/- target-point)
                              (v/* (/ length direction-length))
                              (v/rotate angle)
                              (v/+ target-point))
        calculated-point (-> real-origin
                             (v/- target-point)
                             (v/* (/ (- length part-length) direction-length))
                             (v/rotate angle)
                             (v/+ target-point))]
    {:origin calculated-origin
     :point calculated-point
     :thickness (* x 2)}))

(defn calculate-properties-for-thickness [environment origin anchor
                                          {:keys [size stretch]}
                                          thickness-base base-angle]
  (let [thickness (-> size
                      ((util/percent-of thickness-base)))
        anchor-type (or (:type anchor)
                        :edge)
        {real-origin :real-origin
         real-anchor :real-anchor} (angle/calculate-origin-and-anchor
                                    environment
                                    origin
                                    anchor
                                    thickness
                                    base-angle)
        target-point (case anchor-type
                       :edge (v/find-intersection
                              real-origin real-anchor
                              environment)
                       real-anchor)
        direction-vector (v/- target-point real-origin)
        direction-length (v/abs direction-vector)
        direction (v// direction-vector direction-length)
        length (or stretch 1)
        point (-> direction
                  (v/* (* direction-length length))
                  (v/+ real-origin))]
    {:origin real-origin
     :point point
     :thickness thickness}))

(defn calculate-properties [environment origin anchor
                            {:keys [size-mode]
                             :as geometry} thickness-base base-angle]
  (let [calculate-properties-fn (case size-mode
                                  :angle calculate-properties-for-angle
                                  calculate-properties-for-thickness)]
    (calculate-properties-fn
     environment origin anchor
     geometry thickness-base base-angle)))
