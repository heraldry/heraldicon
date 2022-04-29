(ns heraldicon.coat-of-arms.charge.type.roundel
  (:require
   [heraldicon.coat-of-arms.charge.interface :as charge.interface]
   [heraldicon.coat-of-arms.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/roundel)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/roundel)

(defmethod interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(defmethod charge.interface/render-charge charge-type
  [context]
  (charge.shared/make-charge
   context
   :width
   (fn [width]
     (let [radius (/ width 2)]
       {:shape ["m" (v/v radius 0)
                ["a" radius radius
                 0 0 0 (v/v (- width) 0)]
                ["a" radius radius
                 0 0 0 width 0]
                "z"]
        :charge-width width
        :charge-height width}))))
