(ns or.coad.line
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [or.coad.svg :as svg]))

(defn straight [length _]
  {:line ["l" [length 0]]
   :length length})

(defn invected [length _]
  (let [width 10
        radius (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["a" radius radius 0 0 1 [width 0]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn engrailed [length _]
  (let [width 10
        radius (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["a" radius radius 0 0 0 [radius (- radius)]
                 "a" radius radius 0 0 0 [radius radius]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn embattled [length _]
  (let [width 10
        half-width (/ width 2)
        quarter-width (/ width 4)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["l" [quarter-width 0] [0 (- half-width)] [half-width 0] [0 half-width] [quarter-width 0]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn indented [length _]
  (let [width 10
        half-width (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["l" [half-width (- half-width)] [half-width half-width]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn dancetty [length _]
  (let [width 20
        half-width (/ width 2)
        quarter-width (/ width 4)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["l" [quarter-width (- quarter-width)] [half-width half-width] [quarter-width (- quarter-width)]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn wavy [length reversed?]
  (let [width 20
        half-width (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["a" half-width width 0 0 (if reversed? 0 1) [half-width 0]
                 "a" half-width width 0 0 (if reversed? 1 0) [half-width 0]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn dovetailed [length _]
  (let [width 10
        half-width (/ width 2)
        third-width (/ width 3)
        sixth-width (/ width 6)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> ["l"
                 [third-width 0]
                 [(- sixth-width) (- half-width)]
                 [third-width 0]
                 [third-width 0]
                 [(- sixth-width) half-width]
                 [third-width 0]]
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn raguly [length reversed?]
  (let [width 10
        half-width (/ width 2)
        quarter-width (/ width 4)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> (if reversed?
                  ["l"
                   [quarter-width 0]
                   [quarter-width (- half-width)]
                   [half-width 0]
                   [(- quarter-width) half-width]
                   [quarter-width 0]]
                  ["l"
                   [quarter-width 0]
                   [(- quarter-width) (- half-width)]
                   [half-width 0]
                   [quarter-width half-width]
                   [quarter-width 0]])
                (repeat repetitions)
                (apply concat)
                vec)
     :length (* repetitions width)}))

(defn urdy [length reversed?]
  (let [width 10
        quarter-width (/ width 4)
        eighth-width (/ width 8)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    {:line (->> (if reversed?
                  ["l"
                   [0 eighth-width]
                   [quarter-width quarter-width]
                   [quarter-width (- quarter-width)]
                   [0 (- quarter-width)]
                   [quarter-width (- quarter-width)]
                   [quarter-width quarter-width]
                   [0 eighth-width]]
                  ["l"
                   [0 eighth-width]
                   [quarter-width quarter-width]
                   [quarter-width (- quarter-width)]
                   [0 (- quarter-width)]
                   [quarter-width (- quarter-width)]
                   [quarter-width quarter-width]
                   [0 eighth-width]])
                (repeat repetitions)
                (apply concat)
                vec)
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
              [key name]))))

(defn create [kind length & {:keys [angle reversed? flipped? extra] :or {extra 50}}]
  (let [line ((get kinds-function-map kind)
              (+ length extra) reversed?)]
    (update line :line
            #(-> %
                 svg/make-path
                 (->>
                  (str "M 0,0 "))
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
