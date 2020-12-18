(ns or.coad.svg)

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
