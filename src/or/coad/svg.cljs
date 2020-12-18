(ns or.coad.svg
  (:require [clojure.string :as s]))

(defn new-path [d]
  (let [p (js/document.createElementNS "http://www.w3.org/2000/svg" "path")]
    (.setAttribute p "d" d)
    p))

(defn points [^js/SVGPath path n]
  (let [length (.getTotalLength path)]
    (mapv (fn [i]
            (let [p (.getPointAtLength path (-> length (* i) (/ n)))]
              [(.-x p) (.-y p)])) (range n))))

(defn min-max-x-y [[[x y] & rest]]
  (reduce (fn [[min-x max-x min-y max-y] [x y]]
            [(min min-x x)
             (max max-x x)
             (min min-y y)
             (max max-y y)])
          [x x y y]
          rest))

(defn bounding-box [d]
  (let [path   (new-path d)
        points (points path 1000)
        box    (min-max-x-y points)]
    box))

(defn s [[x y]]
  (str x "," y))

(defn make-path [coll]
  (s/join " " (map (fn [v]
                     (if (string? v)
                       v
                       (s/join "," (map str v)))) coll)))

(def -current-id
  (atom 0))

(defn id [prefix]
  (str prefix (swap! -current-id inc)))

(defn translate [[x y] [dx dy]]
  [(+ x dx)
   (+ y dy)])

(defn scale [[x y] f]
  [(* x f)
   (* y f)])
