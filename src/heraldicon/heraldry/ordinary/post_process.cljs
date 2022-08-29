(ns heraldicon.heraldry.ordinary.post-process
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.humetty :as humetty]
   [heraldicon.heraldry.ordinary.voided :as voided]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]))

(defn process-humetty-properties [{:keys [line opposite-line extra-line
                                          humetty-percentage-base humetty]
                                   :as properties} context]
  (let [humetty (or humetty
                    (some-> (interface/get-sanitized-data (c/++ context :humetty))
                            (update :distance (partial math/percent-of humetty-percentage-base))))]
    (if (:humetty? humetty)
      (cond-> (assoc properties :humetty humetty)
        opposite-line (update :opposite-line merge (select-keys line [:fimbriation :effective-height]))
        extra-line (update :extra-line merge (select-keys line [:fimbriation :effective-height])))
      properties)))

(defn process-voided-properties [{:keys [voided-percentage-base voided]
                                  :as properties} context]
  (let [voided (or voided
                   (some-> (interface/get-sanitized-data (c/++ context :voided))
                           (update :thickness (partial math/percent-of voided-percentage-base))))]
    (if (:voided? voided)
      (assoc properties :voided voided)
      properties)))

(defn properties [{:keys [line-length]
                   :as properties} context]
  (let [{:keys [width height]} (interface/get-parent-environment context)
        fimbriation-percentage-base (min width height)]
    (-> properties
        (update :line (fn [line]
                        (or line (some-> (interface/get-sanitized-data (c/++ context :line))
                                         (line/resolve-percentages line-length fimbriation-percentage-base)))))
        (update :opposite-line (fn [line]
                                 (or line
                                     (some-> (interface/get-sanitized-data (c/++ context :opposite-line))
                                             (line/resolve-percentages line-length fimbriation-percentage-base)))))
        (update :extra-line (fn [line]
                              (or line
                                  (some-> (interface/get-sanitized-data (c/++ context :extra-line))
                                          (line/resolve-percentages line-length fimbriation-percentage-base)))))
        (process-humetty-properties context)
        (process-voided-properties context))))

(defn- process-shape-humetty [{:keys [shape]
                               :as shape-data} context {:keys [humetty]}]
  (if (:humetty? humetty)
    (let [parent-shape (interface/get-exact-parent-shape context)
          adjusted-shape (humetty/coup shape parent-shape humetty)]
      (assoc shape-data
             :shape adjusted-shape
             :lines [{:edge-paths adjusted-shape}]))
    shape-data))

(defn- process-shape-voided [{:keys [shape]
                              :as shape-data} context {:keys [voided]}]
  (if (:voided? voided)
    (let [parent-shape (interface/get-exact-parent-shape context)
          adjusted-shape (voided/void shape parent-shape voided)]
      (-> shape-data
          (assoc :shape adjusted-shape)
          ;; TODO: bit hacky, the code here needs to know the last shape path was
          ;; added and then add it as an additional line
          (update :lines conj {:edge-paths [(last adjusted-shape)]})))
    shape-data))

(defn shape [shape-data context properties]
  (-> shape-data
      (process-shape-humetty context properties)
      (process-shape-voided context properties)))
