(ns heraldicon.svg.path-data-test
  (:require
   ["paper" :as paper]
   [cljs.test :refer-macros [deftest is testing use-fixtures]]
   [heraldicon.svg.path-data :as path-data]))

;; the comparison paths need a paper.js project to live in
(use-fixtures :once
  {:before (fn []
             (doto (paper/PaperScope.)
               (.setup (paper/Size. 500 500))))})

(def ^:private paths
  ["M0,0 L10,0 L10,10 Z"
   "m10,10 l5,5 h10 v-3 z"
   "M1,2 C3,4 5,6 7,8 S9,10 11,12 Q13,14 15,16 T17,18 Z"
   "M0,0 c1.5,-2e1 .5.5 -3,4 s1,1 2,2 q-1,2 3,4 t1,1"
   "M0,0 L10,0 L10,10 L0,0 Z"
   "M0,0 C5,-5 15,-5 20,0 C15,5 5,5 0,0 Z"
   "M0,0 L1,1 M5,5 L6,6"
   "M0,0 L1,1 Z L5,5"
   "M10 10 20 20 30 10"
   "M0,0 A10,10 0 0 1 20,0 L20,20 Z"
   "M-3.25,7.125 C-1.5,9.875 2.75,9.875 4.5,7.125 L4.5,-7.125 C2.75,-9.875 -1.5,-9.875 -3.25,-7.125 Z"
   "M3,3"
   ""])

(defn- paper-path [data]
  (paper/Path. data))

(defn- paper-bounds [^js path]
  (let [bounds (.-bounds path)]
    [(.-x bounds) (.-y bounds) (.-width bounds) (.-height bounds)]))

(deftest same-as-paper
  (doseq [data paths]
    (testing data
      (is (= (path-data/to-svg (path-data/parse data)) (.-pathData (paper-path data))))
      (is (= (path-data/bounds (path-data/parse data)) (paper-bounds (paper-path data))))
      (let [^js expected (doto (paper-path data)
                           (.scale -1.5 0.75 (paper/Point. 0 0))
                           (.rotate 33 (paper/Point. 2.5 -1.25))
                           (.translate 7.5 -3))
            path (-> (path-data/parse data)
                     (path-data/scale -1.5 0.75)
                     (path-data/rotate 33 {:x 2.5 :y -1.25})
                     (path-data/translate 7.5 -3))]
        (is (= (path-data/to-svg path) (.-pathData expected)))
        (is (= (path-data/bounds path) (paper-bounds expected)))))))

(deftest not-a-string
  (is (= (path-data/to-svg (path-data/parse nil)) ""))
  (is (= (path-data/bounds (path-data/parse nil)) [0 0 0 0])))
