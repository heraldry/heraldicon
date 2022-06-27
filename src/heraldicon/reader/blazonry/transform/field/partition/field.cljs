(ns heraldicon.reader.blazonry.transform.field.partition.field
  (:require
   [heraldicon.reader.blazonry.transform.ordinal] ;; needed for side effects
   [heraldicon.reader.blazonry.transform.shared :refer [ast->hdn get-child filter-nodes]]))

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
  (let [number (some-> (get-child #{:ordinal} nodes)
                       ast->hdn)
        location (some-> (get-child #{:field-location} nodes)
                         ast->hdn)]
    (or number
        location)))

(defmethod ast->hdn :partition-field [[_ & nodes]]
  (let [field (ast->hdn (get-child #{:field :plain} nodes))
        references (->> nodes
                        (filter-nodes #{:field-reference})
                        (map ast->hdn))]
    {:references references
     :field field}))
