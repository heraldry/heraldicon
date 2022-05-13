(ns heraldicon.heraldry.charge.type.mascle
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/mascle)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/mascle)

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
           hole-width (* width 0.55)
           hole-height (* height 0.55)
           hole-width-half (/ hole-width 2)
           hole-height-half (/ hole-height 2)]
       {:shape {:paths [["m" (v/Vector. 0 (- height-half))
                         "l" (v/Vector. width-half height-half)
                         "l " (v/Vector. (- width-half) height-half)
                         "l" (v/Vector. (- width-half) (- height-half))
                         "z"]
                        ["m" (v/Vector. 0 (- hole-height-half))
                         "l" (v/Vector. hole-width-half hole-height-half)
                         "l " (v/Vector. (- hole-width-half) hole-height-half)
                         "l" (v/Vector. (- hole-width-half) (- hole-height-half))
                         "z"]]}
        :charge-width width
        :charge-height height}))))
