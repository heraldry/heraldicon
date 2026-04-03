(ns heraldicon.heraldry.line.type.trefly
  (:require
   [heraldicon.math.vector :as v]
   [heraldicon.svg.path :as path]))

(defn scale-vectors [vs width height-factor]
  (let [stretch (v/Vector. 1 height-factor)]
    (into {}
          (map (fn [[k v]]
                 [k (-> v
                        (v/mul width)
                        (v/dot stretch))]))
          vs)))

(def padding
  0.1)

(defn add-padding [v padding]
  (let [stretch (- 1 (* 2 padding))]
    (v/mul v stretch)))

(defn morph [from to t]
  (into {}
        (map (fn [k]
               (let [from-v (get from k)
                     to-v (get to k)]
                 [k (-> (v/sub to-v from-v)
                        (v/mul t)
                        (v/add from-v)
                        ; flip y
                        (v/dot (v/Vector. 1 -1))
                        ; map to 1x1 (the original vectors are for 100x100)
                        (v/mul 0.01)
                        ; add padding on both sides
                        (add-padding padding))])))
        (keys from)))

(def normal-vectors
  {:base (v/Vector. 0 0)
   :base-anchor-out (v/Vector. 50 0)
   :stem-anchor-in (v/Vector. 44 28)
   :stem (v/Vector. 44 29)
   :stem-anchor-out (v/Vector. 44 29)
   :leaf-side-0-anchor-in (v/Vector. 40 14)
   :leaf-side-0 (v/Vector. 23 14)
   :leaf-side-0-anchor-out (v/Vector. 8 14)
   :leaf-side-1-anchor-in (v/Vector. 0 26)
   :leaf-side-1 (v/Vector. 0 36)
   :leaf-side-1-anchor-out (v/Vector. 0 58)
   :leaf-side-2-anchor-in (v/Vector. 24 65)
   :leaf-side-2 (v/Vector. 39 55)
   :leaf-side-2-anchor-out (v/Vector. 39 55)
   :leaf-middle-0-anchor-in (v/Vector. 26 65)
   :leaf-middle-0 (v/Vector. 26 78)
   :leaf-middle-0-anchor-out (v/Vector. 26 86)
   :leaf-middle-1-anchor-in (v/Vector. 33 100)
   :leaf-middle-1 (v/Vector. 50 100)
   :middle (v/Vector. 50 0)
   :effective-height (v/Vector. 0 100)})

(def low-eccentricity-vectors
  {:base (v/Vector. 0 0)
   :base-anchor-out (v/Vector. 50 0)
   :stem-anchor-in (v/Vector. 44 28)
   :stem (v/Vector. 44 29)
   :stem-anchor-out (v/Vector. 44 29)
   :leaf-side-0-anchor-in (v/Vector. 33 21)
   :leaf-side-0 (v/Vector. 17 20)
   :leaf-side-0-anchor-out (v/Vector. 3 21)
   :leaf-side-1-anchor-in (v/Vector. 0 25)
   :leaf-side-1 (v/Vector. 4 35)
   :leaf-side-1-anchor-out (v/Vector. 10 48)
   :leaf-side-2-anchor-in (v/Vector. 32 53)
   :leaf-side-2 (v/Vector. 38 54)
   :leaf-side-2-anchor-out (v/Vector. 38 54)
   :leaf-middle-0-anchor-in (v/Vector. 36 60)
   :leaf-middle-0 (v/Vector. 37 74)
   :leaf-middle-0-anchor-out (v/Vector. 37 86)
   :leaf-middle-1-anchor-in (v/Vector. 41 100)
   :leaf-middle-1 (v/Vector. 50 100)
   :middle (v/Vector. 50 0)
   :effective-height (v/Vector. 0 100)})

(def high-eccentricity-vectors
  {:base (v/Vector. 0 0)
   :base-anchor-out (v/Vector. 50 0)
   :stem-anchor-in (v/Vector. 44 28)
   :stem (v/Vector. 44 29)
   :stem-anchor-out (v/Vector. 44 29)
   :leaf-side-0-anchor-in (v/Vector. 36 19)
   :leaf-side-0 (v/Vector. 27 20)
   :leaf-side-0-anchor-out (v/Vector. 21 20)
   :leaf-side-1-anchor-in (v/Vector. 10 26)
   :leaf-side-1 (v/Vector. 23 38)
   :leaf-side-1-anchor-out (v/Vector. 6 50)
   :leaf-side-2-anchor-in (v/Vector. 20 62)
   :leaf-side-2 (v/Vector. 39 54)
   :leaf-side-2-anchor-out (v/Vector. 39 54)
   :leaf-middle-0-anchor-in (v/Vector. 31 64)
   :leaf-middle-0 (v/Vector. 30 68)
   :leaf-middle-0-anchor-out (v/Vector. 25 82)
   :leaf-middle-1-anchor-in (v/Vector. 37 93)
   :leaf-middle-1 (v/Vector. 50 78)
   :middle (v/Vector. 50 0)
   :effective-height (v/Vector. 0 100)})

(def pattern
  {:display-name :string.line.type/trefly
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [vectors (if (< eccentricity 0.5)
                               (morph normal-vectors low-eccentricity-vectors (- 1 (* 2 eccentricity)))
                               (morph normal-vectors high-eccentricity-vectors (* 2 (- eccentricity 0.5))))
                     {:keys [base
                             base-anchor-out
                             stem-anchor-in
                             stem
                             stem-anchor-out
                             leaf-side-0-anchor-in
                             leaf-side-0
                             leaf-side-0-anchor-out
                             leaf-side-1-anchor-in
                             leaf-side-1
                             leaf-side-1-anchor-out
                             leaf-side-2-anchor-in
                             leaf-side-2
                             leaf-side-2-anchor-out
                             leaf-middle-0-anchor-in
                             leaf-middle-0
                             leaf-middle-0-anchor-out
                             leaf-middle-1-anchor-in
                             leaf-middle-1
                             middle
                             effective-height]} (scale-vectors vectors width height)]
                 {:pattern ["l" (* width padding) 0
                            (path/arc base base-anchor-out stem-anchor-in stem)

                            (path/arc stem stem-anchor-out leaf-side-0-anchor-in leaf-side-0)
                            (path/arc leaf-side-0 leaf-side-0-anchor-out leaf-side-1-anchor-in leaf-side-1)
                            (path/arc leaf-side-1 leaf-side-1-anchor-out leaf-side-2-anchor-in leaf-side-2)

                            (path/arc leaf-side-2 leaf-side-2-anchor-out leaf-middle-0-anchor-in leaf-middle-0)
                            (path/arc leaf-middle-0 leaf-middle-0-anchor-out leaf-middle-1-anchor-in leaf-middle-1)
                            (path/arc leaf-middle-1 leaf-middle-1-anchor-in leaf-middle-0-anchor-out leaf-middle-0 :mirror-at middle)
                            (path/arc leaf-middle-0 leaf-middle-0-anchor-in leaf-side-2-anchor-out leaf-side-2 :mirror-at middle)

                            (path/arc leaf-side-2 leaf-side-2-anchor-in leaf-side-1-anchor-out leaf-side-1 :mirror-at middle)
                            (path/arc leaf-side-1 leaf-side-1-anchor-in leaf-side-0-anchor-out leaf-side-0 :mirror-at middle)
                            (path/arc leaf-side-0 leaf-side-0-anchor-in stem-anchor-out stem :mirror-at middle)

                            (path/arc stem stem-anchor-in base-anchor-out base :mirror-at middle)

                            "l" (* width padding) 0]

                  :min (:y effective-height)
                  :max 0}))})
