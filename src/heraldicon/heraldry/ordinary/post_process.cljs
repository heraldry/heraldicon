(ns heraldicon.heraldry.ordinary.post-process
  (:require
   [heraldicon.heraldry.ordinary.humetty :as humetty]
   [heraldicon.heraldry.ordinary.voided :as voided]
   [heraldicon.interface :as interface]))

(defn properties [{:keys [humetty line opposite-line extra-line]
                   :as properties}]
  (if (:humetty? humetty)
    (cond-> properties
      opposite-line (update :opposite-line merge (select-keys line [:fimbriation :effective-height]))
      extra-line (update :extra-line merge (select-keys line [:fimbriation :effective-height])))
    properties))

(defn- process-humetty [{:keys [shape]
                         :as shape-data} context {:keys [humetty]}]
  (if (:humetty? humetty)
    (let [parent-shape (interface/get-exact-parent-shape context)
          adjusted-shape (humetty/coup-2 shape parent-shape humetty)]
      (assoc shape-data
             :shape adjusted-shape
             :lines [{:edge-paths adjusted-shape}]))
    shape-data))

(defn- process-voided [{:keys [shape]
                        :as shape-data} context {:keys [voided]}]
  (if (:voided? voided)
    (let [parent-shape (interface/get-exact-parent-shape context)
          adjusted-shape (voided/void-2 shape parent-shape voided)]
      (-> shape-data
          (assoc :shape adjusted-shape)
          ;; TODO: bit hacky, the code here needs to know the last shape path was
          ;; added and then add it as an additional line
          (update :lines conj {:edge-paths [(last adjusted-shape)]})))
    shape-data))

(defn shape [shape-data context properties]
  (-> shape-data
      (process-humetty context properties)
      (process-voided context properties)))
