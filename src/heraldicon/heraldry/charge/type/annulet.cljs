(ns heraldicon.heraldry.charge.type.annulet
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]))

(def charge-type :heraldry.charge.type/annulet)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/annulet)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(def ^:private base
  (let [width 100
        radius (/ width 2)
        hole-radius (* radius 0.6)]
    {:base-shape [["M" radius 0
                   "a" radius radius 0 0 0 (- width) 0
                   "a" radius radius 0 0 0 width 0
                   "z"]
                  ["M" hole-radius 0
                   "a" hole-radius hole-radius 0 0 0 (* hole-radius -2) 0
                   "a" hole-radius hole-radius 0 0 0 (* hole-radius 2) 0
                   "z"]]
     :base-width width
     :base-height width}))

(defmethod interface/properties charge-type [context]
  (charge.shared/process-shape context base))
