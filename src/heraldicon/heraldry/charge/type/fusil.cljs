(ns heraldicon.heraldry.charge.type.fusil
  (:require
   [heraldicon.heraldry.charge.interface :as charge.interface]
   [heraldicon.heraldry.charge.shared :as charge.shared]
   [heraldicon.interface :as interface]))

(def charge-type :heraldry.charge.type/fusil)

(defmethod charge.interface/display-name charge-type [_] :string.charge.type/fusil)

(defmethod charge.interface/options charge-type [context]
  (-> (charge.shared/options context)
      (update :geometry dissoc :mirrored?)
      (update :geometry dissoc :reversed?)))

(def ^:private base
  (let [height 100
        width (/ height 2)
        width-half (/ width 2)
        height-half (/ height 2)]
    {:base-shape [["M" 0 (- height-half)
                   "l" width-half height-half
                   "l" (- width-half) height-half
                   "l" (- width-half) (- height-half)
                   "z"]]
     :base-width width
     :base-height height}))

(defmethod interface/properties charge-type [context]
  (charge.shared/process-shape context base))
