(ns or.coad.svg)

(def svg
  (js/document.createElementNS "http://www.w3.org/2000/svg" "svg"))

(defn path [d]
  (let [p (js/document.createElementNS "http://www.w3.org/2000/svg" "path")]
    (.setAttribute p "d" d)
    (.appendChild svg p)
    p))

(defn origin [node]
  (let [point (.createSVGPoint svg)
        ^js/SVGSVGElement parent (.-parentElement node)
        ctm (.getScreenCTM parent)
        transformed (.matrixTransform point ctm)]
    [(.-x transformed) (.-y transformed)]))

(defn bounding-box [d]
  (let [p (path d)
        [ox oy] (origin p)
        box (.getBoundingClientRect p)
        [x y width height] [(.-x box)
                            (.-y box)
                            (.-width box)
                            (.-height box)]]
    (println ox oy [x y width height])))
