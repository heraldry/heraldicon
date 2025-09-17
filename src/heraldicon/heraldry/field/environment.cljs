(ns heraldicon.heraldry.field.environment
  (:require
   ["paper" :as paper]
   ["paperjs-offset" :refer [PaperOffset]]
   [clojure.string :as str]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.util.cache :as cache]
   [taoensso.timbre :as log]))

(defn create
  ([bounding-box] (create bounding-box nil))

  ([{:keys [min-x max-x
            min-y max-y]
     :as bounding-box} points & {:keys [root?]}]
   (let [top-left (v/Vector. min-x min-y)
         top-right (v/Vector. max-x min-y)
         bottom-left (v/Vector. min-x max-y)
         bottom-right (v/Vector. max-x max-y)
         [width height] (bb/size bounding-box)
         top (v/avg top-left top-right)
         bottom (v/avg bottom-left bottom-right)
         ;; not actually center, but chosen such that bend lines at 45° run together in it
         ;; TODO: this needs to be fixed to work with sub-fields, especially those where
         ;; the fess point calculated like this isn't even included in the field
         ;; update: for now only the root environment gets the "smart" fess point, the others
         ;; just get the middle, even if that'll break saltire-like divisions
         center (v/avg top-left bottom-right)
         fess-at-45-deg (v/Vector. (:x top) (+ min-y (/ (- max-x min-x) 2)))
         fess (or (:fess points)
                  (if (and root?
                           (< (:y fess-at-45-deg) (:y center)))
                    fess-at-45-deg
                    center))
         left (v/Vector. min-x (:y fess))
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
     {:bounding-box bounding-box
      :root? root?
      :width width
      :height height
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
               :fly fly}})))

(defn- shape? [shape]
  (not (str/blank? shape)))

(defn- apply-offset [shape distance join]
  (when (shape? shape)
    (let [original-path (paper/Path. shape)
          path (some-> (.offset PaperOffset
                                original-path
                                distance
                                (clj->js {:join join
                                          :insert false}))
                       .-pathData)
          ;; there might be multiple closed paths in the result, find the one with the largest area
          ;; and assume that's the one we want
          sub-paths (some-> path
                            (str/split #"[zZ]"))
          longest (some->> sub-paths
                           (sort-by (fn [path]
                                      (-> (paper/Path. (str path "z"))
                                          .-area
                                          Math/abs)) >)
                           first)]
      (when longest
        (paper/Path. (str longest "z"))))))

(def ^:private shrink-step
  (cache/memoize
   ::shrink-step
   (fn shrink-step [shape distance join]
     (when (shape? shape)
       (let [original-path (paper/Path. shape)
             outline-left (apply-offset shape (- distance) join)]
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
           (.-pathData (apply-offset shape distance join))))))))

(defn shrink-shape [shape distance join]
  (when (shape? shape)
    (let [max-step 1
          join (case join
                 :round "round"
                 :bevel "bevel"
                 "miter")
          shrink-step-fn (fn [shape step join]
                           (try
                             (shrink-step shape step join)
                             (catch :default _
                               (log/error nil "Endless loop while using paperjs-offset"))))]
      (loop [shape shape
             distance distance]
        (cond
          (zero? distance) shape
          (<= distance max-step) (shrink-step-fn shape distance join)
          :else (let [next-shape (shrink-step-fn shape max-step join)]
                  (if next-shape
                    (recur next-shape (- distance max-step))
                    shape)))))))

(defn intersect-shapes [shape1 shape2]
  (when (and (shape? shape1) (shape? shape1))
    (-> (paper/Path. shape1)
        (.intersect (paper/Path. shape2))
        .-pathData)))

(defn subtract-shape [shape1 shape2]
  (if (shape? shape2)
    (when (shape? shape1)
      (-> (paper/Path. shape1)
          (.subtract (paper/Path. shape2))
          .-pathData))
    shape1))
