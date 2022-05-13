(ns heraldicon.heraldry.charge.type.crescent
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/crescent)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/crescent)

(defmethod interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)))

(defmethod charge.interface/render-charge charge-type
  [context]
  (charge.shared/make-charge
   context
   :width
   (fn [width]
     (let [radius (/ width 2)
           inner-radius (* radius
                           0.75)
           horn-angle -45
           horn-point-x (* radius
                           (-> horn-angle
                               (* Math/PI)
                               (/ 180)
                               Math/cos))
           horn-point-y (* radius
                           (-> horn-angle
                               (* Math/PI)
                               (/ 180)
                               Math/sin))
           horn-point-1 (v/Vector. horn-point-x horn-point-y)
           horn-point-2 (v/Vector. (- horn-point-x) horn-point-y)]
       {:shape ["m" horn-point-1
                ["a" radius radius
                 0 1 1 (v/sub horn-point-2 horn-point-1)]
                ["a" inner-radius inner-radius
                 0 1 0 (v/sub horn-point-1 horn-point-2)]
                "z"]
        :charge-width width
        :charge-height width}))))
