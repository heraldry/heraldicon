(ns heraldicon.heraldry.ordinary.post-process
  (:require
   [clojure.set :as set]
   [heraldicon.context :as c]
   [heraldicon.heraldry.line.core :as line]
   [heraldicon.heraldry.ordinary.humetty :as humetty]
   [heraldicon.heraldry.ordinary.voided :as voided]
   [heraldicon.interface :as interface]
   [heraldicon.math.core :as math]
   [heraldicon.svg.squiggly :as squiggly]))

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

(defn line-properties [{:keys [line-length
                               swap-lines?]
                        :as properties} context]
  (let [{:keys [width height]} (interface/get-parent-environment context)
        fimbriation-percentage-base (min width height)]
    (-> properties
        (update :line (fn [line]
                        (or line (some-> (interface/get-sanitized-data (c/++ context :line))
                                         (line/resolve-percentages line-length width height fimbriation-percentage-base)))))
        (update :opposite-line (fn [line]
                                 (or line
                                     (some-> (interface/get-sanitized-data (c/++ context :opposite-line))
                                             (line/resolve-percentages line-length width height fimbriation-percentage-base)))))
        (update :extra-line (fn [line]
                              (or line
                                  (some-> (interface/get-sanitized-data (c/++ context :extra-line))
                                          (line/resolve-percentages line-length width height fimbriation-percentage-base)))))
        (cond->
          swap-lines? (set/rename-keys {:line :opposite-line
                                        :opposite-line :line})))))

(defn properties [properties context]
  (-> properties
      (line-properties context)
      (process-humetty-properties context)
      (process-voided-properties context)))

(defn- process-shape-humetty [{:keys [shape clean-shape]
                               :as shape-data} context {:keys [humetty]}]
  (if (:humetty? humetty)
    (let [parent-shape (interface/get-parent-field-shape context)
          adjusted-shape (humetty/coup shape parent-shape humetty)]
      (cond-> (assoc shape-data
                     :shape adjusted-shape
                     :edges [{:paths adjusted-shape}])
        ;; keep the clean geometry shape in lock-step so exact-shape stays clean
        clean-shape (assoc :clean-shape (humetty/coup clean-shape parent-shape humetty))))
    shape-data))

(defn- process-shape-voided [{:keys [shape clean-shape]
                              :as shape-data} context {:keys [voided]}]
  (if (:voided? voided)
    (let [parent-shape (interface/get-parent-field-shape context)
          adjusted-shape (voided/void shape parent-shape voided)]
      (cond-> (-> shape-data
                  (assoc :shape adjusted-shape)
                  ;; TODO: bit hacky, the code here needs to know the last shape path was
                  ;; added and then add it as an additional line
                  (update :edges conj {:paths [(last adjusted-shape)]}))
        clean-shape (assoc :clean-shape (voided/void clean-shape parent-shape voided))))
    shape-data))

(defn- squiggle-paths [paths]
  (mapv squiggly/squiggly-path paths))

(defn- squiggle-display-shape
  "After couped/voided have re-cut the ordinary against the (clean) parent
  shape, the resulting edges are geometric. Squiggle them for display while
  keeping the clean version in :clean-shape so exact-shape stays geometric.
  Guarded by the caller to only run when humetty/voided actually applied, so
  ordinaries that draw straight from their (already line-squiggled) shape are
  left untouched."
  [{:keys [shape edges] :as shape-data} context]
  (if (interface/render-option :squiggly? context)
    (assoc shape-data
           :clean-shape shape
           :shape (squiggle-paths shape)
           :edges (mapv (fn [edge]
                          (cond-> edge
                            (:paths edge) (update :paths squiggle-paths)))
                        edges))
    shape-data))

(defn shape [{:keys [squiggle-display?]
              :as shape-data} context {:keys [humetty voided]
                                       :as properties}]
  ;; Line-based shapes arrive with both a squiggly :shape and a clean
  ;; :clean-shape (from shape/build-shapes), so nothing further is needed —
  ;; couped/voided above keep both in lock-step. Only point-based shapes that
  ;; carry no clean variant (e.g. the label, flagged squiggle-display?) need a
  ;; squiggly display shape synthesised from their otherwise-clean :shape.
  (let [processed (-> shape-data
                      (dissoc :squiggle-display?)
                      (process-shape-humetty context properties)
                      (process-shape-voided context properties))]
    (cond-> processed
      (and (not (:clean-shape processed))
           (or squiggle-display?
               (:humetty? humetty)
               (:voided? voided))) (squiggle-display-shape context))))
