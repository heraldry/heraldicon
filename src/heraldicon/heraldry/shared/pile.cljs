(ns heraldicon.heraldry.shared.pile
  (:require
   [heraldicon.heraldry.option.position :as position]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]))

(defn diagonals [anchor-point point-point size]
  (let [direction (-> point-point
                      (v/sub anchor-point)
                      v/normal)
        direction-orthogonal (v/orthogonal direction)
        left-point (v/add anchor-point
                          (v/mul direction-orthogonal (/ size 2)))
        right-point (v/sub anchor-point
                           (v/mul direction-orthogonal (/ size 2)))]
    {:left (-> left-point
               (v/sub point-point)
               v/normal
               (v/mul 200)
               (v/add point-point))
     :right (-> right-point
                (v/sub point-point)
                v/normal
                (v/mul 200)
                (v/add point-point))}))

(defn- calculate-angle [target-beta edge-length stretch]
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

(defn- calculate-properties-for-angle [environment shape anchor orientation
                                       {:keys [size stretch]}
                                       _thickness-base base-angle]
  (let [orientation-type (or (:type orientation)
                             :edge)
        alignment (-> anchor :alignment (or :middle))
        {real-anchor :real-anchor
         real-orientation :real-orientation} (position/calculate-anchor-and-orientation
                                              environment
                                              (assoc anchor :alignment :middle)
                                              (assoc orientation :alignment :middle)
                                              0
                                              base-angle)
        target-point (case orientation-type
                       :edge (v/last-intersection-with-shape real-anchor real-orientation shape :default? true)
                       real-orientation)
        direction-vector (v/sub target-point real-anchor)
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
        calculated-anchor (-> real-anchor
                              (v/sub target-point)
                              (v/mul (/ length direction-length))
                              (v/rotate angle)
                              (v/add target-point))
        calculated-point (-> real-anchor
                             (v/sub target-point)
                             (v/mul (/ (- length part-length) direction-length))
                             (v/rotate angle)
                             (v/add target-point))]
    {:anchor calculated-anchor
     :point calculated-point
     :thickness (* x 2)}))

(defn- calculate-properties-for-thickness [environment shape anchor orientation
                                           {:keys [size stretch]}
                                           thickness-base base-angle]
  (let [thickness (math/percent-of thickness-base size)
        orientation-type (or (:type orientation)
                             :edge)
        {real-anchor :real-anchor
         real-orientation :real-orientation} (position/calculate-anchor-and-orientation
                                              environment
                                              anchor
                                              orientation
                                              thickness
                                              base-angle)
        target-point (case orientation-type
                       :edge (v/last-intersection-with-shape real-anchor real-orientation shape :default? true)
                       real-orientation)
        direction-vector (v/sub target-point real-anchor)
        direction-length (v/abs direction-vector)
        direction (v/div direction-vector direction-length)
        length (or stretch 1)
        point (-> direction
                  (v/mul (* direction-length length))
                  (v/add real-anchor))]
    {:anchor real-anchor
     :point point
     :thickness thickness}))

(defn calculate-properties [environment shape anchor orientation
                            {:keys [size-mode]
                             :as geometry} thickness-base base-angle]
  (let [calculate-properties-fn (case size-mode
                                  :angle calculate-properties-for-angle
                                  calculate-properties-for-thickness)]
    (calculate-properties-fn
     environment shape anchor orientation
     geometry thickness-base base-angle)))
