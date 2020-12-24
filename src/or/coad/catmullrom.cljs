(ns or.coad.catmullrom
  (:require [clojure.string :as string]))

;; catmullrom

(defn smooth-component [dir v0 v1 v2 tension]
  (dir v1 (* tension (/ (- v2 v0) 6))))

(defn smooth-point [dir p0 p1 p2 tension]
  (->> (range 2)
       (map (fn [i] (smooth-component dir (p0 i) (p1 i) (p2 i) tension)))
       (vec)))

(defn calculate-cubic-bezier-curve
  [tension [p0 p1 p2 p3]]
  (let [cp1 (smooth-point + p0 p1 p2 tension)
        cp2 (smooth-point - p1 p2 p3 tension)]
    [p1 cp1 cp2 p2]))

(defn catmullrom
  [points & {:keys [tension] :or {tension 1}}]
  (->> (concat [(first points)] points [(last points)])
       (map (juxt :x :y))
       (partition 4 1)
       (map (partial calculate-cubic-bezier-curve tension))))

;; svg

(defn svg-move-to [[x y]]
  (str "M" x "," y))

(defn svg-curve-to-relative [[[px py] [cp1x cp1y] [cp2x cp2y] [p2x p2y]]]
  (str "c" (string/join "," (flatten [[(- cp1x px) (- cp1y py)] [(- cp2x px) (- cp2y py)] [(- p2x px) (- p2y py)]]))))

(defn curve->svg-path-relative [curve]
  (let [start (first (first curve))]
    (string/join "" (concat [(svg-move-to start)]
                            (map svg-curve-to-relative curve)))))
