(ns heraldicon.reader.blazonry.transform.field.partition.field
  (:require
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child transform-first transform-all]]))

(def ^:private field-locations
  {:point/DEXTER :dexter
   :point/SINISTER :sinister
   :point/CHIEF :chief
   :point/BASE :base
   :point/FESS :fess
   :point/DEXTER-CHIEF :chief-dexter
   :point/CHIEF-DEXTER :chief-dexter
   :point/SINISTER-CHIEF :chief-sinister
   :point/CHIEF-SINISTER :chief-sinister
   :point/DEXTER-BASE :base-dexter
   :point/BASE-DEXTER :base-dexter
   :point/SINISTER-BASE :base-sinister
   :point/BASE-SINISTER :base-sinister})

(defmethod ast->hdn :field-location [[_ & nodes]]
  (some-> (get-child field-locations nodes)
          first
          field-locations))

(defmethod ast->hdn :field-reference [[_ & nodes]]
  (or (transform-first #{:ordinal} nodes)
      (transform-first #{:field-location} nodes)))

(defmethod ast->hdn :partition-field [[_ & nodes]]
  {:references (transform-all #{:field-reference} nodes)
   :field (transform-first #{:field :plain} nodes)})
