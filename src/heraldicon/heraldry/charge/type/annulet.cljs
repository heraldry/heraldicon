(ns heraldicon.heraldry.charge.type.annulet
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/annulet)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/annulet)

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
     (let [radius (/ width 2)
           hole-radius (* radius 0.6)]
       {:shape {:paths [["m" (v/Vector. radius 0)
                         ["a" radius radius
                          0 0 0 (v/Vector. (- width) 0)]
                         ["a" radius radius
                          0 0 0 width 0]
                         "z"]
                        ["m" (v/Vector. hole-radius 0)
                         ["a" hole-radius hole-radius
                          0 0 0 (v/Vector. (* hole-radius -2) 0)]
                         ["a" hole-radius hole-radius
                          0 0 0 (* hole-radius 2) 0]
                         "z"]]}
        :charge-width width
        :charge-height width}))))
