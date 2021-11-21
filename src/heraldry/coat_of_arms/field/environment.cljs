(ns heraldry.coat-of-arms.field.environment
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   ["svgpath" :as svgpath]
   [clojure.string :as s]
   [heraldry.math.bounding-box :as bounding-box]
   [heraldry.math.vector :as v]))

(defn create [shape {:keys [bounding-box context] :as meta}]
  (let [shape (if (map? shape)
                shape
                {:paths [shape]})
        [min-x max-x min-y max-y] (or bounding-box
                                      (bounding-box/bounding-box-from-paths
                                       (:paths shape)))
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
    (-> {}
        (assoc :shape shape)
        (assoc :width width)
        (assoc :height height)
        (assoc :meta meta)
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
        (assoc-in [:points :sinister] sinister))))

(defn transform-to-width [environment target-width]
  (let [width (:width environment)
        top-left (get-in environment [:points :top-left])
        offset (v/sub top-left)
        scale-factor (/ target-width width)]
    (-> environment
        (assoc :shape {:paths (into []
                                    (map #(-> %
                                              svgpath
                                              (.translate (:x offset) (:y offset))
                                              (.scale scale-factor)
                                              (.toString)))
                                    (-> environment :shape :paths))})
        (update :width * scale-factor)
        (update :height * scale-factor)
        (update :points merge (into {}
                                    (map (fn [[key value]]
                                           [key (-> value
                                                    (v/add offset)
                                                    (v/mul scale-factor))]) (:points environment)))))))

(defn -shrink-step [shape distance]
  (let [original-path (new Path shape)
        outline-left (-> PaperOffset
                         (.offset
                          original-path
                          (- distance)
                          (clj->js {:join "round"
                                    :insert false})))]
    ;; The path might be clockwise, then (- distance) is the
    ;; correct offset for the inner path; we expect that path
    ;; to be shorter, so use it, if that's true, otherwise
    ;; use the offset on the other side (distance).
    ;; Escutcheon paths are clockwise, so testing for that
    ;; first should avoid having to do both calculations in
    ;; most cases.
    (if (<= (.-length outline-left)
            (.-length original-path))
      (.-pathData outline-left)
      (-> PaperOffset
          (.offset
           original-path
           distance
           (clj->js {:join "round"
                     :insert false}))
          .-pathData))))

(def shrink-step
  (memoize -shrink-step))

(defn shrink-shape [shape distance]
  (let [max-step 5]
    (loop [shape shape
           distance distance]
      (cond
        (zero? distance) shape
        (<= distance max-step) (shrink-step shape distance)
        :else (recur (shrink-step shape max-step) (- distance max-step))))))

(defn intersect-shapes [shape1 shape2]
  (-> (new Path shape1)
      (.intersect (new Path shape2))
      .-pathData))

(defn effective-shape [environment]
  (let [shapes (->> environment
                    (tree-seq map? (comp list :parent-environment :meta))
                    (map :shape)
                    (filter identity))]
    (reduce
     (fn [result shape]
       (let [combined-path (s/join "" (:paths shape))]
         (if (not result)
           combined-path
           (intersect-shapes result combined-path))))
     nil
     shapes)))
