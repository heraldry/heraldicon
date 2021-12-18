(ns heraldry.math.svg.path
  (:require
   ["paper" :refer [Path Point]]
   [clojure.string :as s]
   [heraldry.math.catmullrom :as catmullrom]
   [heraldry.math.vector :as v]))

(defn stitch [path]
  ;; TODO: this can be improved, it already broke some things and caused unexpected behaviour,
  ;; because the 'e' was not part of the pattern
  (s/replace path #"^M[ ]*-?[0-9.e]+[, ] *-?[0-9.e]+" ""))

(defn make-path [v]
  (cond
    (string? v) v
    (and (map? v)
         (:x v)
         (:y v)) (v/->str v)
    (sequential? v) (s/join " " (map make-path v))
    :else (str v)))

(defn parse-path [path]
  (new Path path))

(defn translate [path dx dy]
  (.translate path dx dy))

(defn scale [path sx sy & {:keys [center]
                           :or {center v/zero}}]
  (.scale path sx sy (when center
                       (new Point (:x center) (:y center)))))

(defn rotate [path angle & {:keys [center]
                            :or {center v/zero}}]
  (.rotate path angle (when center
                        (new Point (:x center) (:y center)))))

#_{:clj-kondo/ignore [:redefined-var]}
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
    (v/v (js/parseFloat x) (js/parseFloat y))))

(defn move-to [p]
  (str "M" (v/->str p)))

(defn line-to [p]
  (str " l" (v/->str p) " "))

(defn bezier-to-relative [[p1 cp1 cp2 p2]]
  (str "c" (v/->str (v/sub cp1 p1)) "," (v/->str (v/sub cp2 p1)) "," (v/->str (v/sub p2 p1))))

(defn curve-to-relative [curve]
  (let [start (first (first curve))]
    (s/join "" (concat [(move-to start)]
                       (map bezier-to-relative curve)))))

(defn points [^js/Object path n & {:keys [start-offset]}]
  (let [length (.-length path)
        n (if (= n :length)
            (-> length
                Math/floor
                inc)
            n)]
    (mapv (fn [i]
            (let [x (if start-offset
                      (-> length (* i) (/ (dec n)) (+ start-offset) (mod length))
                      (-> length (* i) (/ (dec n)) (min length)))
                  p (.getPointAt path x)]
              (v/v (.-x p) (.-y p)))) (range n))))

(defn clean-path [d]
  (s/replace d #"l *0 *[, ] *0" ""))

(defn length [path]
  (.-length path))

(defn -sample-path [path n start-offset]
  (-> path
      parse-path
      (points n :start-offset start-offset)))

(def sample-path
  (memoize -sample-path))

(defn -simplify-path [path]
  (-> path
      ;; TODO: the number of sample points could be an option
      (sample-path :length)
      catmullrom/catmullrom
      curve-to-relative))

(def simplify-path
  (memoize -simplify-path))

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

(defn round-corners [path corner-radius]
  (if (zero? corner-radius)
    path
    (let [precision 0.1
          full-length (-> path
                          parse-path
                          length)
          sample-total (-> full-length
                           (/ precision)
                           Math/floor)
          path-points (sample-path path sample-total 0)
          corners (find-corners path-points precision 3)
          diff-index (-> corner-radius
                         (/ precision)
                         Math/floor)]
      (-> (loop [[corner & rest] corners
                 new-path-points []
                 last-index 0
                 final-index (count path-points)]
            (if-let [{:keys [index]} corner]
              (let [start-index (-> index
                                    (- diff-index)
                                    (mod sample-total))
                    end-index (-> index
                                  (+ diff-index)
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
                    patch-path (-> [["M" start-p1]
                                    ["L" start-p2]
                                    ["C"
                                     (-> start-p2
                                         (v/add corner-p)
                                         (v/div 2))
                                     (-> end-p2
                                         (v/add corner-p)
                                         (v/div 2))
                                     end-p2]
                                    ["L" end-p1]]
                                   make-path)
                    rounded-corner-points (sample-path patch-path 50 nil)
                    new-last-index (inc end-index)
                    new-final-index (if (> start-index end-index)
                                      start-index
                                      final-index)
                    new-path-points (-> new-path-points
                                        (cond->
                                          (< start-index end-index) (concat (subvec path-points last-index start-index)))
                                        (concat rounded-corner-points)
                                        vec)]
                (recur rest new-path-points new-last-index new-final-index))
              (do
                (cond-> new-path-points
                  (< last-index final-index) (concat (subvec path-points last-index final-index))))))
          catmullrom/catmullrom
          curve-to-relative))))
