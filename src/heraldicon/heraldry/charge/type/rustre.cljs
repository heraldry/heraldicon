(ns heraldicon.heraldry.charge.type.rustre
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]))

(def charge-type :heraldry.charge.type/rustre)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/rustre)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(def ^:private base
  (let [height 100
        width (/ height 1.3)
        width-half (/ width 2)
        height-half (/ height 2)
        hole-radius (/ width 4)]
    {:base-shape [["M" 0 (- height-half)
                   "l" width-half height-half
                   "l " (- width-half) height-half
                   "l" (- width-half) (- height-half)
                   "z"]
                  ["M" hole-radius 0
                   "a" hole-radius hole-radius 0 0 0 (* hole-radius -2) 0
                   "a" hole-radius hole-radius 0 0 0 (* hole-radius 2) 0
                   "z"]]
     :base-width width
     :base-height height}))

(defmethod interface/properties charge-type [context]
  (charge.shared/process-shape context base))
