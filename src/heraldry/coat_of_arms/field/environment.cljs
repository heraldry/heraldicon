(ns heraldry.coat-of-arms.field.environment
  (:require
   ["svgpath" :as svgpath]
   [heraldry.math.vector :as v]
   [heraldry.math.bounding-box :as bounding-box]))

(defn create [shape {:keys [bounding-box context] :as meta}]
  (let [override-environment (:override-environment meta)
        [min-x max-x min-y max-y] (or bounding-box
                                      (bounding-box/bounding-box-from-path shape))
        top-left (v/v min-x min-y)
        top-right (v/v max-x min-y)
        bottom-left (v/v min-x max-y)
        bottom-right (v/v max-x max-y)
        width (- max-x min-x)
        height (- max-y min-y)
        top (v/avg top-left top-right)
        bottom (v/avg bottom-left bottom-right)
        ;; not actually center, but chosen such that bend lines at 45° run together in it
        ;; TODO: this needs to be fixed to work with sub-fields, especially those where
        ;; the fess point calculated like this isn't even included in the field
        ;; update: for now only the root environment gets the "smart" fess point, the others
        ;; just get the middle, even if that'll break saltire-like divisions
        fess (or (-> meta :points :fess)
                 (if (= context :root)
                   (v/v (:x top) (+ min-y (/ width 2)))
                   (v/avg top-left bottom-right)))
        left (v/v min-x (:y fess))
        right (v/v max-x (:y fess))
        honour (v/avg top fess)
        nombril (v/avg honour bottom)
        chief (v/avg top honour)
        base (v/avg bottom nombril)
        dexter (v/avg left (v/avg left fess))
        sinister (v/avg right (v/avg right fess))]
    (if override-environment
      (-> override-environment
          (assoc :shape shape)
          (assoc :meta meta))
      (-> {}
          (assoc-in [:shape] shape)
          (assoc-in [:width] width)
          (assoc-in [:height] height)
          (assoc-in [:points :top-left] top-left)
          (assoc-in [:points :top-right] top-right)
          (assoc-in [:points :bottom-left] bottom-left)
          (assoc-in [:points :bottom-right] bottom-right)
          (assoc-in [:points :top] top)
          (assoc-in [:points :bottom] bottom)
          (assoc-in [:points :fess] fess)
          (assoc-in [:points :left] left)
          (assoc-in [:points :right] right)
          (assoc-in [:points :honour] honour)
          (assoc-in [:points :nombril] nombril)
          (assoc-in [:points :chief] chief)
          (assoc-in [:points :base] base)
          (assoc-in [:points :dexter] dexter)
          (assoc-in [:points :sinister] sinister)
          (assoc-in [:meta] meta)))))

(defn transform-to-width [environment target-width]
  (let [width (:width environment)
        top-left (get-in environment [:points :top-left])
        offset (v/sub top-left)
        scale-factor (/ target-width width)]
    (-> environment
        (assoc-in [:shape] (-> (:shape environment)
                               (svgpath)
                               (.translate (:x offset) (:y offset))
                               (.scale scale-factor)
                               (.toString)))
        (update-in [:width] * scale-factor)
        (update-in [:height] * scale-factor)
        (update-in [:points] merge (into {}
                                         (map (fn [[key value]]
                                                [key (-> value
                                                         (v/add offset)
                                                         (v/mul scale-factor))]) (:points environment)))))))
