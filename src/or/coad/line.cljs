(ns or.coad.line
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [or.coad.catmullrom :as catmullrom]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn straight [_ length _]
  {:line ["l" [length 0]]
   :start 0
   :length length})

(defn invected [{:keys [eccentricity width offset]
                 :or {eccentricity 1
                      width 10
                      offset 0}} length _]
  (let [radius-x (/ width 2)
        radius-y (* radius-x eccentricity)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["a" radius-x radius-y 0 0 1 [width 0]]
                          (repeat repetitions)))
               (->> (apply merge))
               vec)
     :offset (min 0 offset-length)
     :length (+ offset-length
                (* repetitions width))}))

(defn engrailed [{:keys [eccentricity width offset]
                  :or {eccentricity 1
                       width 10
                       offset 0}} length _]
  (let [radius-x (/ width 2)
        radius-y (* radius-x eccentricity)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["a" radius-x radius-y 0 0 0 [radius-x (- radius-y)]
                           "a" radius-x radius-y 0 0 0 [radius-x radius-y]]
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn embattled [{:keys [eccentricity width offset]
                  :or {eccentricity 1
                       width 10
                       offset 0}} length _]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        height (* eccentricity half-width)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["l"
                           [quarter-width 0]
                           [0 (- height)]
                           [half-width 0]
                           [0 height]
                           [quarter-width 0]]
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn indented [{:keys [eccentricity width offset]
                 :or {eccentricity 1
                      width 10
                      offset 0}} length _]
  (let [half-width (/ width 2)
        height (* eccentricity half-width)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->>
                      [["l"
                        [half-width (- height)]
                        [half-width height]]]
                      (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn dancetty [{:keys [eccentricity width offset]
                 :or {eccentricity 1
                      width 20
                      offset 0}} length _]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        half-height (* quarter-width eccentricity)
        height (* half-height 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["l"
                           [quarter-width (- half-height)]
                           [half-width height]
                           [quarter-width (- half-height)]]
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn wavy [{:keys [eccentricity width offset]
             :or {eccentricity 1
                  width 20
                  offset 0}} length reversed?]
  (let [half-width (/ width 2)
        height (* width eccentricity)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["a" half-width height 0 0 (if reversed? 0 1) [half-width 0]
                           "a" half-width height 0 0 (if reversed? 1 0) [half-width 0]]
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn dovetailed [{:keys [eccentricity width offset]
                   :or {eccentricity 1
                        width 10
                        offset 0}} length _]
  (let [half-width (/ width 2)
        third-width (/ width 3)
        sixth-width (/ width 6)
        height (* half-width eccentricity)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> ["l"
                           [third-width 0]
                           [(- sixth-width) (- height)]
                           [third-width 0]
                           [third-width 0]
                           [(- sixth-width) height]
                           [third-width 0]]
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn raguly [{:keys [eccentricity width offset]
               :or {eccentricity 1
                    width 10
                    offset 0}} length reversed?]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width eccentricity)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> (if reversed?
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
                             [quarter-width 0]])
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

(defn urdy [{:keys [eccentricity width offset]
             :or {eccentricity 1
                  width 10
                  offset 0}} length reversed?]
  (let [quarter-width (/ width 4)
        height (* quarter-width eccentricity)
        half-height (/ height 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int
                        inc)
        offset-length (* offset width)]
    {:line (-> [[(if (pos? offset-length) "l" "m") offset-length 0]]
               (into (->> (if reversed?
                            ["l"
                             [0 half-height]
                             [quarter-width height]
                             [quarter-width (- height)]
                             [0 (- height)]
                             [quarter-width (- height)]
                             [quarter-width height]
                             [0 half-height]]
                            ["l"
                             [0 half-height]
                             [quarter-width height]
                             [quarter-width (- height)]
                             [0 (- height)]
                             [quarter-width (- height)]
                             [quarter-width height]
                             [0 half-height]])
                          (repeat repetitions)))
               (->> (apply concat))
               vec)
     :offset (min 0 offset-length)
     :length (* repetitions width)}))

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
  (let [dist (-> current
                 (v/- previous)
                 (v/abs))
        jiggle-radius (/ dist 4)
        dx (- (* (rand) jiggle-radius)
              jiggle-radius)
        dy (- (* (rand) jiggle-radius)
              jiggle-radius)]
    {:x (+ x dx)
     :y (+ y dy)}))

(defn squiggly-path [path]
  (let [points (-> path
                   svg/new-path
                   (svg/points 100))
        points (vec (concat [(first points)]
                            (map jiggle (partition 3 1 points))
                            [(last points)]))
        curve (catmullrom/catmullrom points)
        new-path (catmullrom/curve->svg-path-relative curve)]
    new-path))

(defn create [line length & {:keys [angle reversed? flipped? extra options] :or {extra 50}}]
  (let [style (or (:style line) :straight)
        line-data ((get kinds-function-map style)
                   line
                   (+ length extra) reversed?)
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
