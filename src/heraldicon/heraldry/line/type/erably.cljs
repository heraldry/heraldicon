(ns heraldicon.heraldry.line.type.erably
  (:require
   [heraldicon.math.vector :as v]))

(def pattern
  {:display-name :string.line.type/erably
   :function (fn [{:keys [height width]}
                  _line-options]
               ;; based on: https://en.wikipedia.org/wiki/Flag_of_Canada#/media/File:Flag_of_Canada_(construction_sheet_-_leaf_geometry).svg
               (let [base-points [-240 -120
                                  -372 -13
                                  -329.678 6.735
                                  -324 15
                                  -322.808 22.534
                                  -360 137
                                  -251.637 113.967
                                  -245 114
                                  -236.968 121.603
                                  -216 171
                                  -131.369 80.245
                                  -117 80
                                  -109.101 91.591
                                  -150 302
                                  -84.543 264.208
                                  -75 263
                                  -66.460 269.565
                                  0 400
                                  66.460 269.565
                                  75 263
                                  84.543 264.208
                                  150 302
                                  109.101 91.591
                                  117 80
                                  131.369 80.245
                                  216 171
                                  236.968 121.603
                                  245 114
                                  251.637 113.967
                                  360 137
                                  322.808 22.534
                                  324 15
                                  329.678 6.735
                                  372 -13
                                  240 -120]
                     base-width 480
                     base-height 400
                     transformed-points (mapv
                                         (fn [[x y]]
                                           (-> (v/Vector. x y)
                                               (v/dot (v/Vector. (/ width base-width 2)
                                                                 (- (/ (* width height) base-height 2))))))
                                         (partition 2 base-points))
                     relative-points (into []
                                           (map (fn [[p1 p2]]
                                                  (v/sub p2 p1)))
                                           (partition 2 1 transformed-points))]
                 {:pattern ["l"
                            relative-points
                            (mapv (fn [p]
                                    (v/dot p (v/Vector. 1 -1)))
                                  relative-points)]
                  :min (- (apply min (map :y transformed-points)))
                  :max (apply min (map :y transformed-points))}))})
