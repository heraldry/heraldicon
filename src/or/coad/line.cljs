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

(defn create [kind length & {:keys [angle reversed? flipped?]}]
  (let [line ((get kinds-function-map kind) length reversed?)]
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
