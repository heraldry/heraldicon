(ns heraldry.math.svg.path
  (:require
   ["paper" :refer [Path Point]]
   [clojure.string :as s]
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
