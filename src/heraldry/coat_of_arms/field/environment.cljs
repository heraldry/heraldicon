(ns heraldry.coat-of-arms.field.environment
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.string :as s]
   [heraldry.math.bounding-box :as bounding-box]
   [heraldry.math.svg.path :as path]
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
    {:shape shape
     :width width
     :height height
     :meta meta
     :points {:top-left top-left
              :top-right top-right
              :bottom-left bottom-left
              :bottom-right bottom-right
              :top top
              :bottom bottom
              :fess fess
              :left left
              :right right
              :honour honour
              :nombril nombril
              :chief chief
              :base base
              :dexter dexter
              :sinister sinister}}))

(defn transform-to-width [environment target-width]
  (let [width (:width environment)
        top-left (get-in environment [:points :top-left])
        offset (v/sub top-left)
        scale-factor (/ target-width width)]
    (-> environment
        (assoc :shape {:paths (into []
                                    (map #(-> %
                                              path/parse-path
                                              (path/translate (:x offset) (:y offset))
                                              (path/scale scale-factor scale-factor)
                                              path/to-svg))
                                    (-> environment :shape :paths))})
        (update :width * scale-factor)
        (update :height * scale-factor)
        (update :points merge (into {}
                                    (map (fn [[key value]]
                                           [key (-> value
                                                    (v/add offset)
                                                    (v/mul scale-factor))]) (:points environment)))))))

(defn -shrink-step [shape distance join]
  (let [original-path (new Path shape)
        outline-left (-> PaperOffset
                         (.offset
                          original-path
                          (- distance)
                          (clj->js {:join join
                                    :insert false})))
        area-smaller? (<= (Math/abs (.-area outline-left))
                          (Math/abs (.-area original-path)))]
    ;; The path might be clockwise, then (- distance) is the
    ;; correct offset for the inner path; we expect that path
    ;; to surround a smaller area, so use it, if that's true, otherwise
    ;; use the offset on the other side (distance).
    ;; Escutcheon paths are clockwise, so testing for that
    ;; first should avoid having to do both calculations in
    ;; most cases.
    (if (or (and area-smaller?
                 (>= distance 0))
            (and (not area-smaller?)
                 (neg? distance)))
      (.-pathData outline-left)
      (-> PaperOffset
          (.offset
           original-path
           distance
           (clj->js {:join join
                     :insert false}))
          .-pathData))))

(def shrink-step
  (memoize -shrink-step))

(defn shrink-shape [shape distance join]
  (let [max-step 5
        join (case join
               :round "round"
               :bevel "bevel"
               "miter")]
    (loop [shape shape
           distance distance]
      (cond
        (zero? distance) shape
        (<= (Math/abs distance) max-step) (shrink-step shape distance join)
        :else (if (pos? distance)
                (recur (shrink-step shape max-step join) (- distance max-step))
                (recur (shrink-step shape (- max-step) join) (+ distance max-step)))))))

(defn intersect-shapes [shape1 shape2]
  (-> (new Path shape1)
      (.intersect (new Path shape2))
      .-pathData))

(defn effective-shape [environment & {:keys [additional-shape]}]
  (let [shapes (->> environment
                    (tree-seq map? (comp list :parent-environment :meta))
                    (map :shape)
                    (filter identity))
        shapes (cond-> shapes
                 additional-shape (conj additional-shape))]
    (reduce
     (fn [result shape]
       (let [combined-path (s/join "" (:paths shape))]
         (if (not result)
           combined-path
           (intersect-shapes result combined-path))))
     nil
     shapes)))
