(ns heraldicon.heraldry.line.type.flory
  (:require
   [heraldicon.math.vector :as v]))

(defn mirror [v]
  (v/dot v (v/Vector. -1 1)))

(defn scale-vectors [vs width height-factor]
  (let [stretch (v/Vector. 1 height-factor)]
    (into {}
          (map (fn [[k v]]
                 [k (-> v
                        (v/mul width)
                        (v/dot stretch))]))
          vs)))

(defn morph [from to t]
  (into {}
        (map (fn [k]
               (let [from-v (get from k)
                     to-v (get to k)]
                 [k (-> (v/sub to-v from-v)
                        (v/mul t)
                        (v/add from-v))])))
        (keys from)))

(def normal-vectors
  {:base-left (v/Vector. 0.3 0)
   :base-right (v/Vector. 0.7 0)
   :point-left (v/Vector. 0.17 -0.13)
   :point-right (v/Vector. 0.83 -0.13)
   :tip (v/Vector. 0.5 -0.75)
   :top-left (v/Vector. 0.45 -0.25)
   :top-right (v/Vector. 0.55 -0.25)
   :anchor1 (v/Vector. 0.2 0)
   :anchor2 (v/Vector. 0.2 -0.2)
   :anchor3 (v/Vector. -0.15 -0.15)
   :anchor4 (v/Vector. -0.15 -0.3)
   :anchor5 (v/Vector. 0 -0.25)
   :anchor6 (v/Vector. -0.25 0.25)})

(def low-eccentricity-vectors
  {:base-left (v/Vector. 0.35 0)
   :base-right (v/Vector. 0.65 0)
   :point-left (v/Vector. 0.17 -0.1)
   :point-right (v/Vector. 0.83 -0.1)
   :tip (v/Vector. 0.5 -0.75)
   :top-left (v/Vector. 0.4 -0.25)
   :top-right (v/Vector. 0.6 -0.25)
   :anchor1 (v/Vector. 0.1 0)
   :anchor2 (v/Vector. 0.3 -0.1)
   :anchor3 (v/Vector. 0 -0.15)
   :anchor4 (v/Vector. -0.2 -0.05)
   :anchor5 (v/Vector. 0 -0.25)
   :anchor6 (v/Vector. -0.1 0.15)})

(def high-eccentricity-vectors
  {:base-left (v/Vector. 0.35 0)
   :base-right (v/Vector. 0.65 0)
   :point-left (v/Vector. 0.25 -0.1)
   :point-right (v/Vector. 0.75 -0.1)
   :tip (v/Vector. 0.5 -0.75)
   :top-left (v/Vector. 0.45 -0.2)
   :top-right (v/Vector. 0.55 -0.2)
   :anchor1 (v/Vector. 0.15 -0.1)
   :anchor2 (v/Vector. -0.0 -0.25)
   :anchor3 (v/Vector. -0.3 -0.2)
   :anchor4 (v/Vector. -0.2 -0.4)
   :anchor5 (v/Vector. 0.05 -0.3)
   :anchor6 (v/Vector. -0.3 0.25)})

(def pattern
  {:display-name :string.line.type/flory
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [vectors (if (< eccentricity 0.5)
                               (morph normal-vectors low-eccentricity-vectors (- 1 (* 2 eccentricity)))
                               (morph normal-vectors high-eccentricity-vectors (* 2 (- eccentricity 0.5))))
                     {:keys [base-left
                             base-right
                             tip
                             top-left
                             top-right
                             point-left
                             point-right
                             anchor1
                             anchor2
                             anchor3
                             anchor4
                             anchor5
                             anchor6]} (scale-vectors vectors width height)]

                 {:pattern ["l"
                            base-left

                            "c"
                            anchor1
                            (v/add (v/sub point-left base-left) anchor2)
                            (v/sub point-left base-left)

                            "c"
                            anchor3
                            (v/add (v/sub top-left point-left) anchor4)
                            (v/sub top-left point-left)

                            "c"
                            anchor5
                            (v/add (v/sub tip top-left) anchor6)
                            (v/sub tip top-left)

                            "c"
                            (mirror anchor6)
                            (v/add (v/sub top-right tip) (mirror anchor5))
                            (v/sub top-right tip)

                            "c"
                            (mirror anchor4)
                            (v/add (v/sub point-right top-right) (mirror anchor3))
                            (v/sub point-right top-right)

                            "c"
                            (mirror anchor2)
                            (v/add (v/sub base-right point-right) (mirror anchor1))
                            (v/sub base-right point-right)

                            "l" (- width (:x base-right)) 0]

                  :min (:y tip)
                  :max 0}))})
