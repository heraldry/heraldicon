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
          ;; couping uses the clean geometry (clean-shape, or the plain shape for
          ;; point-based ordinaries like the label); squiggle-display-shape then
          ;; re-squiggles the whole couped result for display, so the couped edge
          ;; wobbles just like the rest of the contour
          adjusted-shape (humetty/coup (or clean-shape shape) parent-shape humetty)]
      (assoc shape-data
             :shape adjusted-shape
             :clean-shape adjusted-shape
             :edges [{:paths adjusted-shape}]))
    shape-data))

(defn- process-shape-voided [{:keys [shape clean-shape]
                              :as shape-data} context {:keys [voided]}]
  (if (:voided? voided)
    (let [parent-shape (interface/get-parent-field-shape context)
          adjusted-shape (voided/void (or clean-shape shape) parent-shape voided)]
      (-> shape-data
          (assoc :shape adjusted-shape
                 :clean-shape adjusted-shape)
          ;; TODO: bit hacky, the code here needs to know the last shape path was
          ;; added and then add it as an additional line
          (update :edges conj {:paths [(last adjusted-shape)]})))
    shape-data))

(defn- squiggle-paths [paths]
  (mapv squiggly/squiggly-path paths))

(defn- squiggle-display-shape
  "Squiggle the (clean) couped/voided shape uniformly for display, keeping the
  clean version in :clean-shape so exact-shape stays geometric. Run only when
  couped/voided actually applied (or for the point-based label via
  squiggle-display?); plain line-based ordinaries keep their per-line squiggly
  :shape from build-shapes and aren't touched here."
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
  ;; A plain line-based ordinary keeps its per-line squiggly :shape + clean
  ;; :clean-shape from build-shapes untouched. When couped/voided (or for the
  ;; point-based label), the shape is re-cut against the clean parent above and
  ;; then squiggled wholesale for display so the couped/voided edge wobbles too.
  (cond-> (-> shape-data
              (dissoc :squiggle-display?)
              (process-shape-humetty context properties)
              (process-shape-voided context properties))
    (or squiggle-display?
        (:humetty? humetty)
        (:voided? voided)) (squiggle-display-shape context)))
