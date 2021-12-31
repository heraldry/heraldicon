(ns heraldry.coat-of-arms.charge.type.lozenge
  (:require
   [heraldry.coat-of-arms.charge.interface :as charge-interface]
   [heraldry.coat-of-arms.charge.shared :as charge-shared]
   [heraldry.interface :as interface]
   [heraldry.math.vector :as v]))

(def charge-type :heraldry.charge.type/lozenge)

(defmethod charge-interface/display-name charge-type [_] :string.charge.type/lozenge)

(defmethod interface/options charge-type [context]
  (-> (charge-shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(defmethod charge-interface/render-charge charge-type
  [context]
  (charge-shared/make-charge
   context
   :height
   (fn [height]
     (let [width (/ height 1.3)
           width-half (/ width 2)
           height-half (/ height 2)]
       {:shape ["m" (v/v 0 (- height-half))
                "l" (v/v width-half height-half)
                "l " (v/v (- width-half) height-half)
                "l" (v/v (- width-half) (- height-half))
                "z"]
        :charge-width width
        :charge-height height}))))
