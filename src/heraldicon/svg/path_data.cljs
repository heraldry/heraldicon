(ns heraldicon.svg.path-data
  "SVG paths as data, for parsing, transforming, bounding and writing paths
  without paper.js objects. It follows paper.js's Path exactly: the same
  parsing, the same matrix arithmetic, the same curve bounds and the same
  written path data. Boolean operations, intersections and lengths stay with
  paper.js.

  A path is {:segments segments :closed? bool}. segments is a flat JS array
  with six numbers per segment, x y in-x in-y out-x out-y, the handles relative
  to their point, as paper.js stores them. Every function returns a new path
  and never changes its input, so parsed paths can be cached and shared."
  (:require
   ["paper" :as paper]
   [heraldicon.util.cache :as cache]))

(def ^:private epsilon 1e-12)
(def ^:private machine-epsilon 1.12e-16)

(defn- segment-count [segments]
  (/ (.-length segments) 6))

;; parsing, as paper.js's Path#setPathData; it builds its array in place,
;; nothing else sees it before it's done

(defn- push-segment! [segments x y in-x in-y out-x out-y]
  (doto segments
    (.push x y in-x in-y out-x out-y)))

(defn- move-to! [segments x y]
  (case (segment-count segments)
    0 (push-segment! segments x y 0 0 0 0)
    1 (do (set! (.-length segments) 0)
          (push-segment! segments x y 0 0 0 0))
    segments))

(defn- cubic-to! [segments h1x h1y h2x h2y x y]
  (when (zero? (.-length segments))
    (throw (ex-info "Use a moveTo() command first" {})))
  (let [base (- (.-length segments) 6)]
    (aset segments (+ base 4) (- h1x (aget segments base)))
    (aset segments (+ base 5) (- h1y (aget segments (inc base))))
    (push-segment! segments x y (- h2x x) (- h2y y) 0 0)))

(defn- quadratic-to! [segments hx hy x y]
  (let [base (- (.-length segments) 6)
        px (aget segments base)
        py (aget segments (inc base))
        third (/ 1 3)]
    (cubic-to! segments
               (+ hx (* (- px hx) third)) (+ hy (* (- py hy) third))
               (+ hx (* (- x hx) third)) (+ hy (* (- y hy) third))
               x y)))

(defn- close! [segments]
  (when (>= (segment-count segments) 2)
    (let [base (- (.-length segments) 6)
          dx (- (aget segments base) (aget segments 0))
          dy (- (aget segments (inc base)) (aget segments 1))]
      (when (<= (js/Math.sqrt (+ (* dx dx) (* dy dy))) 1e-12)
        (aset segments 2 (aget segments (+ base 2)))
        (aset segments 3 (aget segments (+ base 3)))
        (set! (.-length segments) base))))
  segments)

;; the same patterns as paper.js; global, so match returns every hit
(def ^:private command-pattern (js/RegExp. "[mlhvcsqtaz][^mlhvcsqtaz]*" "ig"))
(def ^:private number-pattern (js/RegExp. "[+-]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][+-]?\\d+)?" "g"))

(defn- parse* [data]
  (let [parts (some-> data (.match command-pattern))
        segments #js []]
    (loop [i 0
           closed? false
           cx 0 cy 0
           sx 0 sy 0
           ctrl-x 0 ctrl-y 0
           previous nil]
      (if (or (nil? parts) (>= i (.-length parts)))
        {:segments segments
         :closed? closed?}
        (let [part (aget parts i)
              command (.charAt part 0)
              lower (.toLowerCase command)
              coords (or (.match part number-pattern) #js [])
              n (.-length coords)
              relative? (= command lower)
              coord (fn [j base]
                      (let [value (js/Number (aget coords j))]
                        (if relative? (+ value base) value)))]
          (when (and (= previous "z")
                     (not (#{"m" "z"} lower)))
            (move-to! segments cx cy))
          (case lower
            ("m" "l") (let [[cx cy sx sy]
                            (loop [j 0 cx cx cy cy sx sx sy sy move? (= lower "m")]
                              (if (< j n)
                                (let [x (coord j cx)
                                      y (coord (inc j) cy)]
                                  (if move?
                                    (do (move-to! segments x y)
                                        (recur (+ j 2) x y x y false))
                                    (do (push-segment! segments x y 0 0 0 0)
                                        (recur (+ j 2) x y sx sy false))))
                                [cx cy sx sy]))]
                        (recur (inc i) closed? cx cy sx sy cx cy lower))
            ("h" "v") (let [[cx cy]
                            (loop [j 0 cx cx cy cy]
                              (if (< j n)
                                (let [x (if (= lower "h") (coord j cx) cx)
                                      y (if (= lower "h") cy (coord j cy))]
                                  (push-segment! segments x y 0 0 0 0)
                                  (recur (inc j) x y))
                                [cx cy]))]
                        (recur (inc i) closed? cx cy sx sy cx cy lower))
            "c" (let [[cx cy ctrl-x ctrl-y]
                      (loop [j 0 cx cx cy cy ctrl-x ctrl-x ctrl-y ctrl-y]
                        (if (< j n)
                          (let [h1x (coord j cx) h1y (coord (+ j 1) cy)
                                h2x (coord (+ j 2) cx) h2y (coord (+ j 3) cy)
                                x (coord (+ j 4) cx) y (coord (+ j 5) cy)]
                            (cubic-to! segments h1x h1y h2x h2y x y)
                            (recur (+ j 6) x y h2x h2y))
                          [cx cy ctrl-x ctrl-y]))]
                  (recur (inc i) closed? cx cy sx sy ctrl-x ctrl-y lower))
            "s" (let [[cx cy ctrl-x ctrl-y]
                      (loop [j 0 cx cx cy cy ctrl-x ctrl-x ctrl-y ctrl-y previous previous]
                        (if (< j n)
                          (let [reflect? (#{"c" "s"} previous)
                                h1x (if reflect? (- (* cx 2) ctrl-x) cx)
                                h1y (if reflect? (- (* cy 2) ctrl-y) cy)
                                h2x (coord j cx) h2y (coord (+ j 1) cy)
                                x (coord (+ j 2) cx) y (coord (+ j 3) cy)]
                            (cubic-to! segments h1x h1y h2x h2y x y)
                            (recur (+ j 4) x y h2x h2y lower))
                          [cx cy ctrl-x ctrl-y]))]
                  (recur (inc i) closed? cx cy sx sy ctrl-x ctrl-y lower))
            "q" (let [[cx cy ctrl-x ctrl-y]
                      (loop [j 0 cx cx cy cy ctrl-x ctrl-x ctrl-y ctrl-y]
                        (if (< j n)
                          (let [hx (coord j cx) hy (coord (+ j 1) cy)
                                x (coord (+ j 2) cx) y (coord (+ j 3) cy)]
                            (quadratic-to! segments hx hy x y)
                            (recur (+ j 4) x y hx hy))
                          [cx cy ctrl-x ctrl-y]))]
                  (recur (inc i) closed? cx cy sx sy ctrl-x ctrl-y lower))
            "t" (let [[cx cy ctrl-x ctrl-y]
                      (loop [j 0 cx cx cy cy ctrl-x ctrl-x ctrl-y ctrl-y previous previous]
                        (if (< j n)
                          (let [reflect? (#{"q" "t"} previous)
                                hx (if reflect? (- (* cx 2) ctrl-x) cx)
                                hy (if reflect? (- (* cy 2) ctrl-y) cy)
                                x (coord j cx) y (coord (+ j 1) cy)]
                            (quadratic-to! segments hx hy x y)
                            (recur (+ j 2) x y hx hy lower))
                          [cx cy ctrl-x ctrl-y]))]
                  (recur (inc i) closed? cx cy sx sy ctrl-x ctrl-y lower))
            "z" (do (close! segments)
                    (recur (inc i) true sx sy sx sy ctrl-x ctrl-y lower))))))))

(defn- from-paper
  "Reads the segments of a paper.js path, for the arcs paper.js converts."
  [^js path]
  (let [segments #js []]
    (doseq [^js segment (.-segments path)]
      (let [point (.-point segment)
            handle-in (.-handleIn segment)
            handle-out (.-handleOut segment)]
        (push-segment! segments
                       (.-x point) (.-y point)
                       (.-x handle-in) (.-y handle-in)
                       (.-x handle-out) (.-y handle-out))))
    {:segments segments
     :closed? (boolean (.-closed path))}))

(def ^:private parse-cache
  (cache/lru-cache 4096))

(defn parse
  "The path for SVG path data, as paper.js's Path would read it."
  [data]
  (if-not (string? data)
    {:segments #js []
     :closed? false}
    (or (cache/get parse-cache data)
        (let [path (if (re-find #"[aA]" data)
                     (let [paper-path (paper/Path. data)]
                       (try
                         (from-paper paper-path)
                         (finally
                           (.remove paper-path))))
                     (parse* data))]
          (cache/put parse-cache data path)
          path))))

;; transforming, as paper.js's Segment#_transformCoordinates with change

(defn- near-zero? [x y]
  (and (<= (- epsilon) x epsilon)
       (<= (- epsilon) y epsilon)))

(defn transform
  "Applies the affine matrix [a b c d tx ty] to the path."
  [{:keys [segments closed?]} [a b c d tx ty]]
  (let [n (.-length segments)
        result (js/Array. n)]
    (loop [base 0]
      (when (< base n)
        (let [x (aget segments base)
              y (aget segments (+ base 1))
              ix (aget segments (+ base 2))
              iy (aget segments (+ base 3))
              ox (aget segments (+ base 4))
              oy (aget segments (+ base 5))
              nx (+ (* x a) (* y c) tx)
              ny (+ (* x b) (* y d) ty)]
          (aset result base nx)
          (aset result (+ base 1) ny)
          (if (near-zero? ix iy)
            (do (aset result (+ base 2) ix)
                (aset result (+ base 3) iy))
            (let [ax (+ ix x)
                  ay (+ iy y)]
              (aset result (+ base 2) (- (+ (* ax a) (* ay c) tx) nx))
              (aset result (+ base 3) (- (+ (* ax b) (* ay d) ty) ny))))
          (if (near-zero? ox oy)
            (do (aset result (+ base 4) ox)
                (aset result (+ base 5) oy))
            (let [ax (+ ox x)
                  ay (+ oy y)]
              (aset result (+ base 4) (- (+ (* ax a) (* ay c) tx) nx))
              (aset result (+ base 5) (- (+ (* ax b) (* ay d) ty) ny))))
          (recur (+ base 6)))))
    {:segments result
     :closed? closed?}))

(defn translate [path dx dy]
  (transform path [1 0 0 1 dx dy]))

(defn scale
  "Scales around the origin, as path/scale does."
  [path sx sy]
  (transform path [sx 0 0 sy 0 0]))

(defn rotate
  "Rotates by angle degrees around center, as paper.js's Matrix#rotate."
  [path angle {:keys [x y]}]
  (let [angle (* angle (/ js/Math.PI 180))
        cos (js/Math.cos angle)
        sin (js/Math.sin angle)
        tx (+ (- x (* x cos)) (* y sin))
        ty (- (- y (* x sin)) (* y cos))]
    (transform path [cos sin (- sin) cos tx ty])))

;; bounds, as paper.js's Path.getBounds, Curve._addBounds and
;; Numerical.solveQuadratic

(defn- split-double [value]
  (let [x (* value 134217729)
        y (- value x)
        hi (+ y x)]
    [hi (- value hi)]))

(defn- discriminant [a b c]
  (let [d (- (* b b) (* a c))
        e (+ (* b b) (* a c))]
    (if (< (* (js/Math.abs d) 3) e)
      (let [[ah al] (split-double a)
            [bh bl] (split-double b)
            [ch cl] (split-double c)
            p (* b b)
            dp (+ (- (* bh bh) p) (* 2 bh bl) (* bl bl))
            q (* a c)
            dq (+ (- (* ah ch) q) (* ah cl) (* al ch) (* al cl))]
        (+ (- p q) (- dp dq)))
      d)))

(defn- normalization-factor [a b c]
  (let [norm (max a b c)]
    (if (and (not= norm 0)
             (or (< norm 1e-8) (> norm 1e8)))
      (js/Math.pow 2 (- (js/Math.round (js/Math.log2 norm))))
      0)))

(defn- solve-quadratic!
  "Writes the real roots of a*x^2 + b*x + c into roots and returns their
  number, as paper.js finds them."
  [a b c roots]
  (let [abs js/Math.abs
        x1+x2 (if (< (abs a) epsilon)
                (when-not (< (abs b) epsilon)
                  #js [(/ (- c) b) js/Infinity])
                (let [b (* b -0.5)
                      d (discriminant a b c)
                      f (if (and (not= d 0) (< (abs d) machine-epsilon))
                          (normalization-factor (abs a) (abs b) (abs c))
                          0)
                      [a b c d] (if (not= f 0)
                                  (let [a (* a f) b (* b f) c (* c f)]
                                    [a b c (discriminant a b c)])
                                  [a b c d])]
                  (when (>= d (- machine-epsilon))
                    (let [q (if (< d 0) 0 (js/Math.sqrt d))
                          r (+ b (if (< b 0) (- q) q))]
                      (if (= r 0)
                        #js [(/ c a) (- (/ c a))]
                        #js [(/ r a) (/ c r)])))))]
    (if (nil? x1+x2)
      0
      (let [x1 (aget x1+x2 0)
            x2 (aget x1+x2 1)
            count (if (js/isFinite x1)
                    (do (aset roots 0 x1) 1)
                    0)]
        (if (and (not= x2 x1) (js/isFinite x2))
          (do (aset roots count x2) (inc count))
          count)))))

(defn- add-bounds!
  "Widens the [min max] at offset in min-max for one curve coordinate."
  [min-max offset roots v0 v1 v2 v3]
  (let [min-v (aget min-max offset)
        max-v (aget min-max (inc offset))
        add! (fn [value]
               (when (< value (aget min-max offset))
                 (aset min-max offset value))
               (when (> value (aget min-max (inc offset)))
                 (aset min-max (inc offset) value)))]
    (when (or (< v0 min-v) (< v1 min-v) (< v2 min-v) (< v3 min-v)
              (> v0 max-v) (> v1 max-v) (> v2 max-v) (> v3 max-v))
      (if (and (not= (< v1 v0) (< v1 v3))
               (not= (< v2 v0) (< v2 v3)))
        (do (add! v0)
            (add! v3))
        (let [a (+ (- (* 3 (- v1 v2)) v0) v3)
              b (- (* 2 (+ v0 v2)) (* 4 v1))
              c (- v1 v0)
              count (solve-quadratic! a b c roots)]
          (add! v3)
          (dotimes [i count]
            (let [t (aget roots i)
                  u (- 1 t)]
              (when (<= 1e-8 t (- 1 1e-8))
                (add! (+ (* u u u v0)
                         (* 3 u u t v1)
                         (* 3 u t t v2)
                         (* t t t v3)))))))))))

(defn bounds
  "The bounds of the path's curves as [x y width height], like paper.js's
  Rectangle."
  [{:keys [segments closed?]}]
  (let [n (.-length segments)]
    (if (zero? n)
      [0 0 0 0]
      (let [min-max #js [(aget segments 0) (aget segments 0)
                         (aget segments 1) (aget segments 1)]
            roots #js [0 0]
            ;; the curve from the segment at prev to the one at base
            curve! (fn [prev base]
                     (let [px (aget segments prev)
                           py (aget segments (+ prev 1))
                           x (aget segments base)
                           y (aget segments (+ base 1))]
                       (add-bounds! min-max 0 roots
                                    px (+ (aget segments (+ prev 4)) px)
                                    (+ (aget segments (+ base 2)) x) x)
                       (add-bounds! min-max 2 roots
                                    py (+ (aget segments (+ prev 5)) py)
                                    (+ (aget segments (+ base 3)) y) y)))]
        (loop [base 6]
          (when (< base n)
            (curve! (- base 6) base)
            (recur (+ base 6))))
        (when closed?
          (curve! (- n 6) 0))
        (let [min-x (aget min-max 0)
              max-x (aget min-max 1)
              min-y (aget min-max 2)
              max-y (aget min-max 3)]
          [min-x min-y (- max-x min-x) (- max-y min-y)])))))

;; writing, as paper.js's Path#getPathData

(defn- rounded [value]
  (/ (js/Math.round (* value 100000)) 100000))

(defn- pair [x y]
  (str (rounded x) "," (rounded y)))

(defn to-svg
  "The SVG path data for the path, as paper.js writes it."
  [{:keys [segments closed?]}]
  (let [n (.-length segments)]
    (if (zero? n)
      ""
      (let [parts #js []
            ;; the previous point and its absolute out handle
            state #js [0 0 0 0]
            add-segment! (fn [base first? skip-line?]
                           (let [x (aget segments base)
                                 y (aget segments (+ base 1))
                                 in-x (+ (aget segments (+ base 2)) x)
                                 in-y (+ (aget segments (+ base 3)) y)]
                             (if first?
                               (.push parts (str "M" (pair x y)))
                               (let [prev-x (aget state 0)
                                     prev-y (aget state 1)
                                     out-x (aget state 2)
                                     out-y (aget state 3)]
                                 (if (and (= in-x x) (= in-y y)
                                          (= out-x prev-x) (= out-y prev-y))
                                   (when-not skip-line?
                                     (let [dx (- x prev-x)
                                           dy (- y prev-y)]
                                       (.push parts (cond
                                                      (= dx 0) (str "v" (rounded dy))
                                                      (= dy 0) (str "h" (rounded dx))
                                                      :else (str "l" (pair dx dy))))))
                                   (.push parts (str "c" (pair (- out-x prev-x) (- out-y prev-y))
                                                     " " (pair (- in-x prev-x) (- in-y prev-y))
                                                     " " (pair (- x prev-x) (- y prev-y)))))))
                             (aset state 0 x)
                             (aset state 1 y)
                             (aset state 2 (+ (aget segments (+ base 4)) x))
                             (aset state 3 (+ (aget segments (+ base 5)) y))))]
        (loop [base 0]
          (when (< base n)
            (add-segment! base (zero? base) false)
            (recur (+ base 6))))
        (when closed?
          (add-segment! 0 false true)
          (.push parts "z"))
        (.join parts "")))))
