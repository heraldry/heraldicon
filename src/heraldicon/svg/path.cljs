(ns heraldicon.svg.path
  (:refer-clojure :exclude [reverse])
  (:require
   ["paper" :as paper]
   [clojure.string :as str]
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.vector :as v]
   [heraldicon.util.cache :as cache]))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (str/replace path #"^M[ ]*-?[0-9.e]+[, ] *-?[0-9.e]+" ""))

(defn make-path [v]
  (cond
    (string? v) v
    (and (map? v)
         (:x v)
         (:y v)) (v/->str v)
    (sequential? v) (str/join " " (map make-path v))
    :else (str v)))

(defn parse-path [path]
  (paper/Path. path))

(defn translate
  ([path {:keys [x y]}]
   (.translate path x y))
  ([path dx dy]
   (.translate path dx dy)))

(defn scale [path sx sy & {:keys [center]
                           :or {center v/zero}}]
  (.scale path sx sy (when center
                       (paper/Point. (:x center) (:y center)))))

(defn rotate [path angle & {:keys [center]
                            :or {center v/zero}}]
  (.rotate path angle (when center
                        (paper/Point. (:x center) (:y center)))))

(defn reverse [path]
  (doto path .reverse))

(defn to-svg [^js/Object path & {:keys [relative?
                                        from-zero?]}]
  (cond-> (.-pathData path)
    (or relative?
        from-zero?) stitch
    from-zero? (->> (str "M0,0 "))))

(defn get-start-pos [^js/Object path]
  (let [path-data (.-pathData path)
        regex #"^M[ ]*(-?[0-9.e]+)[, ] *(-?[0-9.e]+).*"
        [_ x y] (re-matches regex path-data)]
    (v/Vector. (js/parseFloat x) (js/parseFloat y))))

(defn- move-to [p]
  (str "M" (v/->str p)))

(defn line-to [p]
  (str " l" (v/->str p) " "))

(defn bezier-to-relative [[p1 cp1 cp2 p2]]
  (str "c" (v/->str (v/sub cp1 p1)) "," (v/->str (v/sub cp2 p1)) "," (v/->str (v/sub p2 p1))))

(defn curve-to-relative [curve]
  (let [start (ffirst curve)
        parts (js/Array. (move-to start))]
    (doseq [[{x1 :x y1 :y} cp1 cp2 p2] curve]
      (.push parts (str "c"
                        (- (:x cp1) x1) "," (- (:y cp1) y1) ","
                        (- (:x cp2) x1) "," (- (:y cp2) y1) ","
                        (- (:x p2) x1) "," (- (:y p2) y1))))
    (.join parts "")))

(defn- points-with-offset [^js/Object path n start-offset]
  ;; Slow path: wrapping arc-length offset, used by round-corners/clockwise?.
  ;; Keeps the original getPointAt approach since it's not the squiggle hot path.
  (let [total-length (.-length path)
        n-dec (dec n)]
    (loop [i 0
           acc (transient [])]
      (if (< i n)
        (let [x (-> total-length (* i) (/ n-dec) (+ start-offset) (mod total-length))
              p (.getPointAt path x)]
          (recur (inc i)
                 (if p
                   (conj! acc (v/Vector. (.-x p) (.-y p)))
                   acc)))
        (persistent! acc)))))

(defn- points-uniform [^js/Object path n]
  ;; Fast path: no offset, uniform arc-length sampling.
  ;; Walks the curves array once with a single forward pointer to find which
  ;; curve each sample falls on, then calls curve.getTimeAt(within-curve-offset)
  ;; for the arc-length → t conversion on that curve alone. This avoids the
  ;; full-path linear scan that path.getPointAt does on every call (O(curves)
  ;; per sample), while producing bit-identical sample positions.
  (let [curves (.getCurves path)
        num-curves (.-length curves)
        ;; Collect per-curve lengths (Paper.js caches _length after first call).
        curve-lengths (js/Array. num-curves)
        total-length (loop [ci 0
                            total 0.0]
                       (if (< ci num-curves)
                         (let [cl ^js (.getLength (aget curves ci))]
                           (aset curve-lengths ci cl)
                           (recur (inc ci) (+ total cl)))
                         total))
        step (/ total-length (dec n))]
    ;; Single forward pass: advance ci (curve index) and si (sample index)
    ;; together. curve-end tracks the arc-length at the end of the current curve.
    (loop [ci 0
           curve-end (aget curve-lengths 0)
           si 0
           acc (transient [])]
      (cond
        (= si n)
        (persistent! acc)

        ;; Advance to the next curve when the current sample arc-offset has
        ;; passed the end of this curve (and it's not the last sample).
        (and (< ci (dec num-curves))
             (> (* si step) curve-end))
        (let [next-ci (inc ci)]
          (recur next-ci
                 (+ curve-end (aget curve-lengths next-ci))
                 si
                 acc))

        :else
        (let [curve (aget curves ci)
              cl (aget curve-lengths ci)
              curve-start (- curve-end cl)
              arc-offset (* si step)
              within-curve (- arc-offset curve-start)
              ;; getTimeAt converts arc-length offset within this curve to the
              ;; bezier t parameter using Newton's method — same as getPointAt
              ;; does internally, but scoped to just this curve, not the whole path.
              t (if (< cl 1e-10)
                  0.0
                  (min 1.0 (max 0.0 ^js (.getTimeAt curve within-curve))))
              p ^js (.getPointAtTime curve t)]
          (recur ci curve-end (inc si) (conj! acc (v/Vector. (.-x p) (.-y p)))))))))

(defn points [^js/Object path n & {:keys [start-offset]}]
  (let [total-length (.-length path)
        n (if (= n :length)
            (-> total-length
                Math/floor
                inc)
            n)]
    (cond
      start-offset (points-with-offset path n start-offset)
      (> n 1) (points-uniform path n)
      :else (let [p (.getPointAt path 0)]
              (if p [(v/Vector. (.-x p) (.-y p))] [])))))

(defn length [path]
  (.-length path))

(def sample-path
  (cache/memoize
   ::sample-path
   (fn sample-path [path & {:keys [start-offset
                                   precision
                                   num-points]}]
     (let [parsed-path (parse-path path)
           full-length (length parsed-path)
           n (or num-points
                 (-> full-length
                     (/ precision)
                     Math/floor))]
       (points parsed-path n :start-offset start-offset)))))

(def ^:private simplify-path
  (cache/memoize
   ::simplify-path
   (fn simplify-path [path smoothing]
     (-> path
         (sample-path :precision smoothing)
         catmullrom/catmullrom
         curve-to-relative))))

(defn clockwise? [path]
  (let [points (sample-path path :num-points 20)]
    (->> (conj points (first points))
         (partition 2 1)
         (map (fn [[{x1 :x y1 :y}
                    {x2 :x y2 :y}]]
                (* (- x2 x1) (+ y1 y2))))
         (reduce + 0)
         ;; we're in an upside down cartesian system, so negative
         ;; here means clockwise, the case 0 means the path doesn't
         ;; have a dominant clockwise or counter-clockwise portion,
         ;; so "false" also is the correct value for that case
         neg?)))

(defn find-corners [points precision detection-radius]
  (let [d (/ detection-radius precision)
        sample-total (count points)
        cutoff-angle 45
        cutoff-dot-product (-> cutoff-angle
                               (* Math/PI)
                               (/ 180)
                               Math/cos
                               Math/abs)
        deduping-index-radius d
        raw-corners (->> (range sample-total)
                         (map (fn [index]
                                (let [p (get points index)
                                      p1 (get points (mod (- index d) sample-total))
                                      p2 (get points (mod (+ index d) sample-total))
                                      arm1 (v/sub p1 p)
                                      arm2 (v/sub p2 p)
                                      dot-product (v/dot-product arm1 arm2)]
                                  [index dot-product])))
                         (reduce (fn [aggregator [index dot-product]]
                                   (if (< (Math/abs dot-product) cutoff-dot-product)
                                     (if-let [{last-index :index
                                               last-dot-product :dot-product} (last aggregator)]
                                       (if (<= (- index last-index)
                                               deduping-index-radius)
                                         (if (< (Math/abs dot-product) (Math/abs last-dot-product))
                                           (-> aggregator
                                               pop
                                               (conj {:index index
                                                      :dot-product dot-product}))
                                           aggregator)
                                         (conj aggregator {:index index
                                                           :dot-product dot-product}))
                                       [{:index index
                                         :dot-product dot-product}])
                                     aggregator))
                                 []))
        first-corner (first raw-corners)
        last-corner (last raw-corners)]
    (if (and (> (count raw-corners) 1)
             (-> (:index first-corner)
                 (+ sample-total)
                 (- (:index last-corner))
                 Math/abs
                 (<= deduping-index-radius)))
      (if (< (:dot-product first-corner)
             (:dot-product last-corner))
        (-> raw-corners
            drop-last
            vec)
        (->> raw-corners
             drop-last
             (drop 1)
             (concat [(last raw-corners)])
             vec))
      raw-corners)))

(defn- add-max-radius-to-corners [corners num-points]
  (case (count corners)
    0 []
    1 (let [corner (first corners)]
        [(assoc corner
                :max-radius-left (-> num-points
                                     (quot 2)
                                     dec)
                :max-radius-right (-> num-points
                                      (quot 2)
                                      dec))])
    (->> (concat [(last corners)] corners [(first corners)])
         (partition 3 1)
         (mapv (fn [[{previous-index :index} {index :index :as corner} {next-index :index}]]
                 (assoc corner
                        :max-radius-left (-> index
                                             (- previous-index)
                                             (mod num-points)
                                             (quot 2)
                                             dec)
                        :max-radius-right (-> next-index
                                              (- index)
                                              (mod num-points)
                                              (quot 2)
                                              dec)))))))

(defn round-corners [path corner-radius smoothing]
  (cond-> (if (zero? corner-radius)
            path
            (let [precision 0.1
                  path-points (sample-path path
                                           :start-offset 0
                                           :precision precision)
                  sample-total (count path-points)
                  corners (add-max-radius-to-corners (find-corners path-points precision 3)
                                                     (count path-points))
                  diff-index (-> corner-radius
                                 (/ precision)
                                 Math/floor)]
              (-> (loop [[corner & rest] corners
                         new-path-points []
                         last-index 0
                         final-index (count path-points)]
                    (if-let [{:keys [index max-radius-left max-radius-right]} corner]
                      (let [start-index (-> index
                                            (- (min diff-index max-radius-left))
                                            (mod sample-total))
                            end-index (-> index
                                          (+ (min diff-index max-radius-right))
                                          (mod sample-total))
                            corner-p (get path-points index)
                            start-p1 (get path-points start-index)
                            start-p2 (get path-points (-> start-index
                                                          inc
                                                          (mod sample-total)))
                            end-p1 (get path-points end-index)
                            end-p2 (get path-points (-> end-index
                                                        dec
                                                        (mod sample-total)))
                            patch-path (make-path
                                        [["M" start-p1]
                                         ["L" start-p2]
                                         ["C"
                                          (-> corner-p
                                              (v/sub start-p2)
                                              (v/mul 0.5)
                                              (v/add start-p2))
                                          (-> corner-p
                                              (v/sub end-p2)
                                              (v/mul 0.5)
                                              (v/add end-p2))
                                          end-p2]
                                         ["L" end-p1]])
                            rounded-corner-points (sample-path patch-path
                                                               :precision precision)
                            new-last-index (inc end-index)
                            new-final-index (if (> start-index end-index)
                                              start-index
                                              final-index)
                            new-path-points (-> new-path-points
                                                (cond->
                                                  (and (<= 0 start-index)
                                                       (< start-index end-index)
                                                       (< end-index (count path-points))) (concat (subvec path-points last-index start-index)))
                                                (concat rounded-corner-points)
                                                vec)]
                        (recur rest new-path-points new-last-index new-final-index))
                      ;; TODO: there's a special case here if the last corner reaches
                      ;; into the beginning of the path, then this algorithm doesn't
                      ;; behave well and the last step pretty much doubles it, because
                      ;; of this "rest" being added, and portion at the start of the
                      ;; path not being removed
                      (cond-> new-path-points
                        (< last-index final-index) (concat (subvec path-points last-index final-index)))))
                  catmullrom/catmullrom
                  curve-to-relative)))
    (pos? smoothing) (simplify-path smoothing)))
