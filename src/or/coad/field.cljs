(ns or.coad.field
  (:require ["svgpath" :as svgpath]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn make-field [shape meta]
  ;; TODO: bounding-box is an expensive operation, if the calling context knows
  ;; the dimensions, then it should pass them down, only use bounding box when necessary
  (let [[min-x max-x min-y max-y] (svg/bounding-box shape)
        top-left                  (v/v min-x min-y)
        top-right                 (v/v max-x min-y)
        bottom-left               (v/v min-x max-y)
        bottom-right              (v/v max-x max-y)
        width                     (- max-x min-x)
        height                    (- max-y min-y)
        chief                     (v/avg top-left top-right)
        base                      (v/avg bottom-left bottom-right)
        ;; not actually center, but chosen such that bend lines at 45° run together in it
        ;; TODO: this needs to be fixed to work with sub-fields, especially those where
        ;; the fess point calculated like this isn't even included in the field
        fess                      (v/v (:x chief) (+ min-y (/ width 2)))
        dexter                    (v/v min-x (:y fess))
        sinister                  (v/v max-x (:y fess))
        honour                    (v/avg chief fess)
        nombril                   (v/avg honour base)]
    (-> {}
        (assoc-in [:shape] shape)
        (assoc-in [:width] width)
        (assoc-in [:height] height)
        (assoc-in [:points :top-left] top-left)
        (assoc-in [:points :top-right] top-right)
        (assoc-in [:points :bottom-left] bottom-left)
        (assoc-in [:points :bottom-right] bottom-right)
        (assoc-in [:points :chief] chief)
        (assoc-in [:points :base] base)
        (assoc-in [:points :fess] fess)
        (assoc-in [:points :dexter] dexter)
        (assoc-in [:points :sinister] sinister)
        (assoc-in [:points :honour] honour)
        (assoc-in [:points :nombril] nombril)
        (assoc-in [:meta] meta))))

(defn transform-to-width [field target-width]
  (let [width        (:width field)
        top-left     (get-in field [:points :top-left])
        offset       (v/- top-left)
        scale-factor (/ target-width width)]
    (-> field
        (assoc-in [:shape] (-> (:shape field)
                               (svgpath)
                               (.translate (:x offset) (:y offset))
                               (.scale scale-factor)
                               (.toString)))
        (update-in [:width] * scale-factor)
        (update-in [:height] * scale-factor)
        (update-in [:points] merge (into {}
                                         (map (fn [[key value]]
                                                [key (-> value
                                                         (v/+ offset)
                                                         (v/* scale-factor))]) (:points field)))))))
