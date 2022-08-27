(ns heraldicon.math.bounding-box
  (:require
   [clojure.string :as s]
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(defrecord ^:export BoundingBox [^js/Number min-x
                                 ^js/Number max-x
                                 ^js/Number min-y
                                 ^js/Number max-y])

(defn combine
  (^BoundingBox [^BoundingBox {min-x1 :min-x max-x1 :max-x
                               min-y1 :min-y max-y1 :max-y}
                 ^BoundingBox {min-x2 :min-x max-x2 :max-x
                               min-y2 :min-y max-y2 :max-y}]
   (BoundingBox. (min min-x1 min-x2)
                 (max max-x1 max-x2)
                 (min min-y1 min-y2)
                 (max max-y1 max-y2)))
  (^BoundingBox [^BoundingBox b1 ^BoundingBox b2 & more]
   (reduce combine (combine b1 b2) more)))

(defn from-vector ^BoundingBox [^v/Vector {x :x y :y}]
  (BoundingBox. x x y y))

(defn from-vector-and-size ^BoundingBox [^v/Vector {x :x y :y} width height]
  (BoundingBox. x (+ x width) y (+ y height)))

(defn from-points ^BoundingBox [[^v/Vector v & more]]
  (reduce combine (from-vector v) (map from-vector more)))

(defn from-paths ^BoundingBox [paths]
  (let [points (mapcat
                #(-> %
                     path/parse-path
                     (path/points 50))
                paths)]
    (from-points points)))

(defn rotate ^BoundingBox [^BoundingBox {x1 :min-x x2 :max-x
                                         y1 :min-y y2 :max-y}
                           ^js/Number rotation & {:keys [^v/Vector middle ^v/Vector scale]}]
  (let [middle (or middle
                   (v/avg (v/Vector. x1 y1) (v/Vector. x2 y2)))
        scale (or scale
                  (v/Vector. 1 1))
        points [(v/add middle
                       (v/rotate (v/dot (v/sub (v/Vector. x1 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/Vector. x2 y1)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/Vector. x1 y2)
                                               middle)
                                        scale) rotation))
                (v/add middle
                       (v/rotate (v/dot (v/sub (v/Vector. x2 y2)
                                               middle)
                                        scale) rotation))]]
    (from-points points)))

(defn scale ^BoundingBox [^BoundingBox bb
                          ^js/Number factor]
  (-> bb
      (update :min-x * factor)
      (update :max-x * factor)
      (update :min-y * factor)
      (update :max-y * factor)))

(defn translate ^BoundingBox [^BoundingBox bb
                              ^v/Vector {:keys [x y]}]
  (-> bb
      (update :min-x + x)
      (update :max-x + x)
      (update :min-y + y)
      (update :max-y + y)))

(defn surrounds? ^Boolean [^BoundingBox {:keys [min-x max-x
                                                min-y max-y]}
                           ^v/Vector {:keys [x y]}]
  (and (<= min-x x max-x)
       (<= min-y y max-y)))

(defn dilate ^BoundingBox [^BoundingBox bb
                           ^js/Number margin]
  (-> bb
      (update :min-x - margin)
      (update :max-x + margin)
      (update :min-y - margin)
      (update :max-y + margin)))

(defn width ^js/Number [^BoundingBox {:keys [min-x max-x]}]
  (- max-x min-x))

(defn height ^js/Number [^BoundingBox {:keys [min-y max-y]}]
  (- max-y min-y))

(defn ->viewbox [{:keys [min-x min-y]
                  :as bounding-box} & {:keys [margin]
                                       :or {margin 0}}]
  (s/join " "
          (map str
               [(- min-x margin)
                (- min-y margin)
                (+ (width bounding-box) (* 2 margin))
                (+ (height bounding-box) (* 2 margin))])))
