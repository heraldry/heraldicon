(ns heraldicon.frontend.canvas-test
  (:require
   ["paper" :as paper]
   [cljs.test :refer-macros [are deftest]]
   [heraldicon.frontend.canvas :as canvas]))

(deftest shape-detection
  (are [width pixels shapes] (= (#'canvas/painted-shapes
                                 (mapv (comp not zero?) pixels)
                                 width) shapes)

    40
    [1 1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
     1 1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0
     1 1 0 1 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 1 1 1 1 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
     1 0 0 1 1 1 0 0 0 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 1 1 1 1 1 1 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 0 0 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 0 0 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 1 0 0 0 0 0 0 0 0 0 1 1 0 1 0 1 0 1 1 0 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 1 1 1 0 0 0 0 0 0 0 0 1 1 0 1 0 0 0 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 1 1 1 1 1 1 1 1 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0 0 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1 1 1 1 1 1 1 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0]
    {0 [[{:x 0 :y 0 :dir :left}
         {:x 0 :y 0 :dir :top}
         {:x 1 :y 0 :dir :top}
         {:x 1 :y 0 :dir :right}
         {:x 2 :y 1 :dir :top}
         {:x 3 :y 1 :dir :top}
         {:x 4 :y 0 :dir :left}
         {:x 4 :y 0 :dir :top}
         {:x 5 :y 0 :dir :top}
         {:x 6 :y 0 :dir :top}
         {:x 6 :y 0 :dir :right}
         {:x 6 :y 1 :dir :right}
         {:x 6 :y 2 :dir :right}
         {:x 6 :y 3 :dir :right}
         {:x 6 :y 4 :dir :right}
         {:x 6 :y 4 :dir :bottom}
         {:x 5 :y 5 :dir :right}
         {:x 6 :y 6 :dir :top}
         {:x 7 :y 6 :dir :top}
         {:x 8 :y 6 :dir :top}
         {:x 9 :y 6 :dir :top}
         {:x 9 :y 6 :dir :right}
         {:x 9 :y 6 :dir :bottom}
         {:x 8 :y 6 :dir :bottom}
         {:x 7 :y 7 :dir :right}
         {:x 7 :y 8 :dir :right}
         {:x 8 :y 9 :dir :top}
         {:x 8 :y 9 :dir :right}
         {:x 8 :y 9 :dir :bottom}
         {:x 7 :y 9 :dir :bottom}
         {:x 6 :y 9 :dir :bottom}
         {:x 6 :y 9 :dir :left}
         {:x 6 :y 9 :dir :top}
         {:x 7 :y 8 :dir :left}
         {:x 7 :y 7 :dir :left}
         {:x 6 :y 6 :dir :bottom}
         {:x 5 :y 6 :dir :bottom}
         {:x 4 :y 6 :dir :bottom}
         {:x 4 :y 6 :dir :left}
         {:x 3 :y 5 :dir :bottom}
         {:x 3 :y 5 :dir :left}
         {:x 3 :y 4 :dir :left}
         {:x 3 :y 4 :dir :top}
         {:x 4 :y 3 :dir :left}
         {:x 3 :y 2 :dir :bottom}
         {:x 2 :y 2 :dir :bottom}
         {:x 1 :y 3 :dir :right}
         {:x 1 :y 4 :dir :right}
         {:x 1 :y 4 :dir :bottom}
         {:x 0 :y 5 :dir :right}
         {:x 0 :y 5 :dir :bottom}
         {:x 0 :y 5 :dir :left}
         {:x 0 :y 4 :dir :left}
         {:x 0 :y 3 :dir :left}
         {:x 0 :y 2 :dir :left}
         {:x 0 :y 1 :dir :left}]
        [{:x 3 :y 4 :dir :right}
         {:x 4 :y 5 :dir :top}
         {:x 5 :y 4 :dir :left}
         {:x 4 :y 3 :dir :bottom}]]
     1 [[{:x 17 :y 0 :dir :left}
         {:x 17 :y 0 :dir :top}
         {:x 17 :y 0 :dir :right}
         {:x 17 :y 1 :dir :right}
         {:x 18 :y 2 :dir :top}
         {:x 19 :y 2 :dir :top}
         {:x 20 :y 2 :dir :top}
         {:x 21 :y 2 :dir :top}
         {:x 22 :y 2 :dir :top}
         {:x 23 :y 2 :dir :top}
         {:x 24 :y 2 :dir :top}
         {:x 25 :y 2 :dir :top}
         {:x 26 :y 2 :dir :top}
         {:x 27 :y 2 :dir :top}
         {:x 28 :y 2 :dir :top}
         {:x 29 :y 2 :dir :top}
         {:x 30 :y 2 :dir :top}
         {:x 30 :y 2 :dir :right}
         {:x 30 :y 3 :dir :right}
         {:x 30 :y 4 :dir :right}
         {:x 30 :y 5 :dir :right}
         {:x 30 :y 6 :dir :right}
         {:x 30 :y 7 :dir :right}
         {:x 30 :y 8 :dir :right}
         {:x 30 :y 9 :dir :right}
         {:x 30 :y 10 :dir :right}
         {:x 30 :y 11 :dir :right}
         {:x 30 :y 12 :dir :right}
         {:x 30 :y 13 :dir :right}
         {:x 30 :y 13 :dir :bottom}
         {:x 29 :y 13 :dir :bottom}
         {:x 28 :y 13 :dir :bottom}
         {:x 27 :y 13 :dir :bottom}
         {:x 26 :y 13 :dir :bottom}
         {:x 25 :y 13 :dir :bottom}
         {:x 24 :y 13 :dir :bottom}
         {:x 23 :y 13 :dir :bottom}
         {:x 22 :y 13 :dir :bottom}
         {:x 21 :y 13 :dir :bottom}
         {:x 20 :y 13 :dir :bottom}
         {:x 19 :y 13 :dir :bottom}
         {:x 18 :y 13 :dir :bottom}
         {:x 17 :y 13 :dir :bottom}
         {:x 17 :y 13 :dir :left}
         {:x 17 :y 12 :dir :left}
         {:x 17 :y 11 :dir :left}
         {:x 17 :y 10 :dir :left}
         {:x 17 :y 9 :dir :left}
         {:x 17 :y 8 :dir :left}
         {:x 17 :y 7 :dir :left}
         {:x 17 :y 6 :dir :left}
         {:x 17 :y 5 :dir :left}
         {:x 17 :y 4 :dir :left}
         {:x 17 :y 3 :dir :left}
         {:x 17 :y 2 :dir :left}
         {:x 17 :y 1 :dir :left}]
        [{:x 18 :y 3 :dir :right}
         {:x 18 :y 4 :dir :right}
         {:x 18 :y 5 :dir :right}
         {:x 18 :y 6 :dir :right}
         {:x 18 :y 7 :dir :right}
         {:x 18 :y 8 :dir :right}
         {:x 18 :y 9 :dir :right}
         {:x 18 :y 10 :dir :right}
         {:x 18 :y 11 :dir :right}
         {:x 19 :y 12 :dir :top}
         {:x 20 :y 12 :dir :top}
         {:x 21 :y 12 :dir :top}
         {:x 22 :y 12 :dir :top}
         {:x 23 :y 12 :dir :top}
         {:x 24 :y 12 :dir :top}
         {:x 25 :y 12 :dir :top}
         {:x 26 :y 12 :dir :top}
         {:x 27 :y 12 :dir :top}
         {:x 28 :y 12 :dir :top}
         {:x 29 :y 11 :dir :left}
         {:x 29 :y 10 :dir :left}
         {:x 29 :y 9 :dir :left}
         {:x 29 :y 8 :dir :left}
         {:x 29 :y 7 :dir :left}
         {:x 29 :y 6 :dir :left}
         {:x 29 :y 5 :dir :left}
         {:x 29 :y 4 :dir :left}
         {:x 29 :y 3 :dir :left}
         {:x 28 :y 2 :dir :bottom}
         {:x 27 :y 2 :dir :bottom}
         {:x 26 :y 2 :dir :bottom}
         {:x 25 :y 2 :dir :bottom}
         {:x 24 :y 2 :dir :bottom}
         {:x 23 :y 2 :dir :bottom}
         {:x 22 :y 2 :dir :bottom}
         {:x 21 :y 2 :dir :bottom}
         {:x 20 :y 2 :dir :bottom}
         {:x 19 :y 2 :dir :bottom}]]
     2 [[{:x 20 :y 4 :dir :left}
         {:x 20 :y 4 :dir :top}
         {:x 21 :y 4 :dir :top}
         {:x 22 :y 4 :dir :top}
         {:x 23 :y 4 :dir :top}
         {:x 24 :y 4 :dir :top}
         {:x 25 :y 4 :dir :top}
         {:x 26 :y 4 :dir :top}
         {:x 27 :y 4 :dir :top}
         {:x 27 :y 4 :dir :right}
         {:x 27 :y 5 :dir :right}
         {:x 27 :y 6 :dir :right}
         {:x 27 :y 7 :dir :right}
         {:x 27 :y 8 :dir :right}
         {:x 27 :y 9 :dir :right}
         {:x 27 :y 10 :dir :right}
         {:x 27 :y 10 :dir :bottom}
         {:x 26 :y 10 :dir :bottom}
         {:x 25 :y 10 :dir :bottom}
         {:x 24 :y 10 :dir :bottom}
         {:x 23 :y 10 :dir :bottom}
         {:x 22 :y 10 :dir :bottom}
         {:x 21 :y 10 :dir :bottom}
         {:x 20 :y 10 :dir :bottom}
         {:x 20 :y 10 :dir :left}
         {:x 20 :y 9 :dir :left}
         {:x 20 :y 8 :dir :left}
         {:x 20 :y 7 :dir :left}
         {:x 20 :y 6 :dir :left}
         {:x 20 :y 5 :dir :left}]
        [{:x 20 :y 5 :dir :right}
         {:x 20 :y 6 :dir :right}
         {:x 20 :y 7 :dir :right}
         {:x 20 :y 8 :dir :right}
         {:x 20 :y 9 :dir :right}
         {:x 21 :y 10 :dir :top}
         {:x 22 :y 10 :dir :top}
         {:x 23 :y 10 :dir :top}
         {:x 24 :y 9 :dir :left}
         {:x 24 :y 8 :dir :left}
         {:x 24 :y 7 :dir :left}
         {:x 24 :y 6 :dir :left}
         {:x 24 :y 5 :dir :left}
         {:x 23 :y 4 :dir :bottom}
         {:x 22 :y 4 :dir :bottom}
         {:x 21 :y 4 :dir :bottom}]
        [{:x 24 :y 6 :dir :right}
         {:x 24 :y 7 :dir :right}
         {:x 25 :y 8 :dir :top}
         {:x 25 :y 8 :dir :right}
         {:x 26 :y 9 :dir :top}
         {:x 27 :y 8 :dir :left}
         {:x 27 :y 7 :dir :left}
         {:x 27 :y 6 :dir :left}
         {:x 26 :y 5 :dir :bottom}
         {:x 25 :y 5 :dir :bottom}]]
     3 [[{:x 22 :y 6 :dir :left}
         {:x 22 :y 6 :dir :top}
         {:x 22 :y 6 :dir :right}
         {:x 22 :y 7 :dir :right}
         {:x 22 :y 8 :dir :right}
         {:x 22 :y 8 :dir :bottom}
         {:x 22 :y 8 :dir :left}
         {:x 22 :y 7 :dir :left}]]}))

(deftest edges-to-path
  (.setup paper (new paper/Size 500 500))

  (are [edges path] (= (#'canvas/edges-to-path edges) path)

    [{:x 1 :y 1 :dir :top}
     {:x 2 :y 0 :dir :left}
     {:x 2 :y 0 :dir :top}
     {:x 3 :y 0 :dir :top}
     {:x 3 :y 0 :dir :right}
     {:x 4 :y 1 :dir :top}
     {:x 4 :y 1 :dir :right}
     {:x 5 :y 2 :dir :top}
     {:x 5 :y 2 :dir :right}
     {:x 5 :y 3 :dir :right}
     {:x 5 :y 4 :dir :right}
     {:x 5 :y 5 :dir :right}
     {:x 5 :y 6 :dir :right}
     {:x 5 :y 6 :dir :bottom}
     {:x 5 :y 6 :dir :left}
     {:x 5 :y 5 :dir :left}
     {:x 5 :y 4 :dir :left}
     {:x 4 :y 3 :dir :bottom}
     {:x 4 :y 3 :dir :left}
     {:x 3 :y 2 :dir :bottom}
     {:x 2 :y 3 :dir :right}
     {:x 2 :y 4 :dir :right}
     {:x 2 :y 4 :dir :bottom}
     {:x 1 :y 5 :dir :right}
     {:x 1 :y 5 :dir :bottom}
     {:x 0 :y 5 :dir :bottom}
     {:x 0 :y 5 :dir :left}
     {:x 0 :y 5 :dir :top}
     {:x 1 :y 4 :dir :left}
     {:x 1 :y 4 :dir :top}
     {:x 2 :y 3 :dir :left}
     {:x 1 :y 2 :dir :bottom}
     {:x 1 :y 2 :dir :left}
     {:x 1 :y 1 :dir :left}]
    "M 1,1 L 2,0 L 4,0 L 6,2 L 6,7 L 5,7 L 4,4 L 4,3 L 3,5 L 2,6 L 0,6 L 0,5 L 1,2z"

    [{:x 1 :y 1 :dir :top}
     {:x 1 :y 1 :dir :right}
     {:x 1 :y 1 :dir :bottom}
     {:x 1 :y 1 :dir :left}]
    "M 1,1 L 2,1 L 2,2 L 1,2z"

    [{:x 1 :y 1 :dir :right}
     {:x 1 :y 1 :dir :bottom}
     {:x 1 :y 1 :dir :left}
     {:x 1 :y 1 :dir :top}]
    "M 2,1 L 2,2 L 1,2 L 1,1z"

    [{:x 1 :y 1 :dir :bottom}
     {:x 1 :y 1 :dir :left}
     {:x 1 :y 1 :dir :top}
     {:x 1 :y 1 :dir :right}]
    "M 2,2 L 1,2 L 1,1 L 2,1z"

    [{:x 1 :y 1 :dir :left}
     {:x 1 :y 1 :dir :top}
     {:x 1 :y 1 :dir :right}
     {:x 1 :y 1 :dir :bottom}]
    "M 1,2 L 1,1 L 2,1 L 2,2z"))
