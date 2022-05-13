(ns heraldicon.heraldry.charge.type.rustre
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/rustre)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/rustre)

(defmethod interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(defmethod charge.interface/render-charge charge-type
  [context]
  (charge.shared/make-charge
   context
   :height
   (fn [height]
     (let [width (/ height 1.3)
           width-half (/ width 2)
           height-half (/ height 2)
           hole-radius (/ width 4)]
       {:shape {:paths [["m" (v/Vector. 0 (- height-half))
                         "l" (v/Vector. width-half height-half)
                         "l " (v/Vector. (- width-half) height-half)
                         "l" (v/Vector. (- width-half) (- height-half))
                         "z"]
                        ["m" (v/Vector. hole-radius 0)
                         ["a" hole-radius hole-radius
                          0 0 0 (v/Vector. (* hole-radius -2) 0)]
                         ["a" hole-radius hole-radius
                          0 0 0 (* hole-radius 2) 0]
                         "z"]]}
        :charge-width width
        :charge-height height}))))
