(ns or.coad.line
  (:require ["svgpath" :as svgpath]
            [clojure.string :as s]
            [or.coad.svg :as svg]))

(defn straight [length]
  ["l" [length 0]])

(defn invected [length]
  (let [width       10
        radius      (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    (->> ["a" radius radius 0 0 1 [width 0]]
         (repeat repetitions)
         (apply concat)
         vec)))
(defn engrailed [length]
  (let [width       10
        radius      (/ width 2)
        repetitions (-> length
                        (/ width)
                        Math/ceil
                        int)]
    (->> ["a" radius radius 0 0 0 [width 0]]
         (repeat repetitions)
         (apply concat)
         vec)))
(def kinds
  [["Straight" :straight straight]
   ["Invected" :invected invected]
   ["Engrailed" :engrailed engrailed]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn create [kind length angle]
  (-> ((get kinds-function-map kind) length)
      svg/make-path
      (->>
       (str "M 0,0 "))
      svgpath
      (.rotate angle)
      .toString))

(defn translate [path dx dy]
  (-> path
      svgpath
      (.translate dx dy)
      .toString))

(defn stitch [path]
  (s/replace path #"^M[0-9.-]+[, ][0-9.-]+" ""))
