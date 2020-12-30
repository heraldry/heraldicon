(ns or.coad.line
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [or.coad.catmullrom :as catmullrom]
            [or.coad.random :as random]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn straight [_ length _]
  {:line   ["l" [length 0]]
   :start  0
   :length length})

(defn line-with-offset [length offset pattern-width pattern]
  (let [offset-length (* offset pattern-width)
        repetitions   (-> length
                          (- offset-length)
                          (/ pattern-width)
                          Math/ceil
                          int
                          inc)]
    {:line   (-> [["l" offset-length 0]]
                 (into (->> pattern
                            (repeat repetitions)))
                 (->> (apply merge))
                 (conj ["l" offset-length 0])
                 vec)
     :offset (min 0 offset-length)
     :length (-> (* repetitions pattern-width)
                 (+ (* offset-length 2)))}))

(defn invected [{:keys [eccentricity width offset]
                 :or   {eccentricity 1
                        width        10
                        offset       0}} length _]
  (let [radius-x (/ width 2)
        radius-y (* radius-x eccentricity)]
    (line-with-offset
     length offset width
     ["a" radius-x radius-y 0 0 1 [width 0]])))

(defn engrailed [{:keys [eccentricity width offset]
                  :or   {eccentricity 1
                         width        10
                         offset       0}} length _]
  (let [radius-x (/ width 2)
        radius-y (* radius-x eccentricity)]
    (line-with-offset
     length offset width
     ["a" radius-x radius-y 0 0 0 [radius-x (- radius-y)]
      "a" radius-x radius-y 0 0 0 [radius-x radius-y]])))

(defn embattled [{:keys [eccentricity width offset]
                  :or   {eccentricity 1
                         width        10
                         offset       0}} length {:keys [reversed?]}]
  (let [half-width (/ width 2)
        height     (* eccentricity half-width)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [half-width 0
         [0 (- height)]
         [half-width 0]
         [0 height]]]
       ["l"
        [0 (- height)]
        [half-width 0]
        [0 height]
        [half-width 0]]))))

(defn indented [{:keys [eccentricity width offset]
                 :or   {eccentricity 1
                        width        10
                        offset       0}} length _]
  (let [half-width (/ width 2)
        height     (* eccentricity half-width)]
    (line-with-offset
     length offset width
     ["l"
      [half-width (- height)]
      [half-width height]])))

(defn dancetty [{:keys [eccentricity width offset]
                 :or   {eccentricity 1
                        width        20
                        offset       0}} length {:keys [reversed?]}]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        half-height   (* quarter-width eccentricity)
        height        (* half-height 2)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [quarter-width half-height]
        [half-width (- height)]
        [quarter-width half-height]]
       ["l"
        [quarter-width (- half-height)]
        [half-width height]
        [quarter-width (- half-height)]]))))

(defn wavy [{:keys [eccentricity width offset]
             :or   {eccentricity 1
                    width        20
                    offset       0}} length  {:keys [reversed?]}]
  (let [half-width (/ width 2)
        height     (* width eccentricity)]
    (line-with-offset
     length offset width
     ["a" half-width height 0 0 (if reversed? 0 1) [half-width 0]
      "a" half-width height 0 0 (if reversed? 1 0) [half-width 0]])))

(defn dovetailed [{:keys [eccentricity width offset]
                   :or   {eccentricity 1
                          width        10
                          offset       0}} length {:keys [reversed?]}]
  (let [half-width  (/ width 2)
        third-width (/ width 3)
        sixth-width (/ width 6)
        height      (* half-width eccentricity)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [(* third-width 2) 0]
        [(- sixth-width) (- height)]
        [third-width 0]
        [third-width 0]
        [(- sixth-width) height]]
       ["l"
        [(- sixth-width) (- height)]
        [third-width 0]
        [third-width 0]
        [(- sixth-width) height]
        [(* third-width 2) 0]]))))

(defn raguly [{:keys [eccentricity width offset]
               :or   {eccentricity 1
                      width        10
                      offset       0}} length  {:keys [reversed?]}]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width eccentricity)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [quarter-width 0]
        [quarter-width (- height)]
        [half-width 0]
        [(- quarter-width) height]
        [quarter-width 0]]
       ["l"
        [quarter-width 0]
        [(- quarter-width) (- height)]
        [half-width 0]
        [quarter-width height]
        [quarter-width 0]]))))

(defn urdy [{:keys [eccentricity width offset]
             :or   {eccentricity 1
                    width        10
                    offset       0}} length  {:keys [reversed?]}]
  (let [quarter-width (/ width 4)
        height        (* quarter-width eccentricity)
        half-height   (/ height 2)]
    (line-with-offset
     length offset width
     (if reversed?
       ["l"
        [0 half-height]
        [quarter-width height]
        [quarter-width (- height)]
        [0 (- height)]
        [quarter-width (- height)]
        [quarter-width height]
        [0 half-height]]
       ["l"
        [0 (- half-height)]
        [quarter-width (- height)]
        [quarter-width height]
        [0 height]
        [quarter-width height]
        [quarter-width (- height)]
        [0 (- half-height)]]))))

(def kinds
  [["Straight" :straight straight]
   ["Invected" :invected invected]
   ["Engrailed" :engrailed engrailed]
   ["Embattled" :embattled embattled]
   ["Indented" :indented indented]
   ["Dancetty" :dancetty dancetty]
   ["Wavy/undy" :wavy wavy]
   ["Dovetailed" :dovetailed dovetailed]
   ["Raguly" :raguly raguly]
   ["Urdy" :urdy urdy]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [name key]))))

(defn jiggle [[previous
               {:keys [x y] :as current}
               _]]
  (let [dist          (-> current
                          (v/- previous)
                          (v/abs))
        jiggle-radius (/ dist 4)
        dx            (- (* (random/float) jiggle-radius)
                         jiggle-radius)
        dy            (- (* (random/float) jiggle-radius)
                         jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn squiggly-path [path]
  (random/seed path)
  (let [points   (-> path
                     svg/new-path
                     (svg/points 100))
        points   (vec (concat [(first points)]
                              (map jiggle (partition 3 1 points))
                              [(last points)]))
        curve    (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(defn create [line length & {:keys [angle flipped? extra options] :or {extra 50} :as line-options}]
  (let [style         (or (:style line) :straight)
        line-data     ((get kinds-function-map style)
                       line
                       (+ length extra) line-options)
        adjusted-path (-> (:line line-data)
                          svg/make-path
                          (->>
                           (str "M 0,0 "))
                          (cond->
                              (:squiggly? options) squiggly-path))]
    (assoc line-data :line
           (-> adjusted-path
               svgpath
               (cond->
                   flipped? (.scale 1 -1))
               (.rotate angle)
               .toString))))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  (s/replace path #"^M[, ]*[0-9.-]+[, ][, ]*[0-9.-]+" ""))
