(ns heraldicon.heraldry.field.environment
  (:require
   ["paper" :refer [Path]]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.string :as s]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(defn create [shape {:keys [bounding-box context
                            width height] :as meta}]
  (let [shape (cond
                (nil? shape) nil
                (map? shape) shape
                :else {:paths [shape]})
        {:keys [min-x max-x
                min-y max-y]} (or bounding-box
                                  (bb/from-paths
                                   (:paths shape)))
        top-left (v/Vector. min-x min-y)
        top-right (v/Vector. max-x min-y)
        bottom-left (v/Vector. min-x max-y)
        bottom-right (v/Vector. max-x max-y)
        width (or width
                  (- max-x min-x))
        height (or height
                   (- max-y min-y))
        top (v/avg top-left top-right)
        bottom (v/avg bottom-left bottom-right)
        ;; not actually center, but chosen such that bend lines at 45° run together in it
        ;; TODO: this needs to be fixed to work with sub-fields, especially those where
        ;; the fess point calculated like this isn't even included in the field
        ;; update: for now only the root environment gets the "smart" fess point, the others
        ;; just get the middle, even if that'll break saltire-like divisions
        fess (or (-> meta :points :fess)
                 (if (= context :root)
                   (v/Vector. (:x top) (+ min-y (/ (- max-x min-x) 2)))
                   (v/avg top-left bottom-right)))
        left (v/Vector. min-x (:y fess))
        center (v/Vector. (/ (+ min-x max-x) 2)
                          (/ (+ min-y max-y) 2))
        right (v/Vector. max-x (:y fess))
        honour (v/avg top fess)
        nombril (v/avg honour bottom)
        chief (v/avg top honour)
        base (v/avg bottom nombril)
        dexter (v/avg left (v/avg left fess))
        sinister (v/avg right (v/avg right fess))
        hoist (v/Vector. (min (+ min-x (/ (- max-y min-y) 2))
                              (:x center)) (:y center))
        fly (v/Vector. (max (- max-x (/ (- max-y min-y) 2))
                            (:x center)) (:y center))]
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
              :center center
              :right right
              :honour honour
              :nombril nombril
              :chief chief
              :base base
              :dexter dexter
              :sinister sinister
              :hoist hoist
              :fly fly}}))

(defn transform-to-width [environment target-width]
  (let [width (:width environment)
        top-left (v/add (get-in environment [:points :top-left])
                        (or (get-in environment [:meta :offset])
                            v/zero))
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
                                                    (v/mul scale-factor))]))
                                    (:points environment)))
        (update-in [:meta :bounding-box] (fn [bb]
                                           (-> bb
                                               (bb/translate offset)
                                               (bb/scale scale-factor)))))))

(def ^:private shrink-step
  (memoize
   (fn shrink-step [shape distance join]
     (let [original-path (new Path shape)
           outline-left (.offset PaperOffset
                                 original-path
                                 (- distance)
                                 (clj->js {:join join
                                           :insert false}))]
    ;; The path might be clockwise, then (- distance) is the
    ;; correct offset for the inner path; we expect that path
    ;; to surround a smaller area, so use it, if that's true, otherwise
    ;; use the offset on the other side (distance).
    ;; Escutcheon paths are clockwise, so testing for that
    ;; first should avoid having to do both calculations in
    ;; most cases.
       (if (<= (Math/abs (.-area outline-left))
               (Math/abs (.-area original-path)))
         (.-pathData outline-left)
         (-> PaperOffset
             (.offset
              original-path
              distance
              (clj->js {:join join
                        :insert false}))
             .-pathData))))))

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
        (<= distance max-step) (shrink-step shape distance join)
        :else (recur (shrink-step shape max-step join) (- distance max-step))))))

(defn intersect-shapes [shape1 shape2]
  (-> (new Path shape1)
      (.intersect (new Path shape2))
      .-pathData))
