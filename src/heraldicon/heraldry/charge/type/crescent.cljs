(ns heraldicon.heraldry.charge.type.crescent
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]))

(def charge-type :heraldry.charge.type/crescent)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/crescent)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)))

(def ^:private base
  (let [width 100
        radius-1 (/ width 2)
        radius-2 (* 0.75 radius-1)
        horn-angle -45
        horn-point-1 (v/rotate (v/Vector. radius-1 0) horn-angle)
        horn-point-2 (v/dot horn-point-1 (v/Vector. -1 1))]
    {:base-shape [["M" horn-point-1
                   "a" radius-1 radius-1 0 1 1 (v/sub horn-point-2 horn-point-1)
                   "a" radius-2 radius-2 0 1 0 (v/sub horn-point-1 horn-point-2)
                   "z"]]
     :base-width width
     :base-height width}))

(defmethod interface/properties charge-type [context]
  (charge.shared/process-shape context base))
