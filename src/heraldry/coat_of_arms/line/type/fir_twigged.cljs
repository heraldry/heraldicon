(ns heraldry.coat-of-arms.line.type.fir-twigged)

(defn pattern
  {:display-name "Fir-twigged"
   :value :fir-twigged}
  [{:keys [height width]}
   _line-options]
  (let [sqrt2 (Math/sqrt 2)
        sqrt2-half (/ sqrt2 2)
        dx (/ width (+ 4 (* 2 sqrt2)))
        dy (* dx height)]
    ["l"
     [(- dx) (- dy)]
     [0 (- dy)]
     [dx 0]
     [dx dy]
     [0 (* -2 dy)]
     [(* dx sqrt2-half) (- (* dy sqrt2-half))]
     [(* dx sqrt2-half) (* dy sqrt2-half)]
     [0 (* 2 dy)]
     [dx (- dy)]
     [dx 0]
     [0 dy]
     [(* -2 dx) (* 2 dy)]
     [0 dy]
     [dx 0]
     [dx (- dy)]
     [0 (* 2 dy)]
     [(* dx sqrt2-half) (* dy sqrt2-half)]
     [(* dx sqrt2-half) (- (* dy sqrt2-half))]
     [0 (* -2 dy)]
     [dx dy]
     [dx 0]
     [0 (- dy)]
     [(- dx) (- dy)]]))
