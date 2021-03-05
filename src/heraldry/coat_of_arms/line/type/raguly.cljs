(ns heraldry.coat-of-arms.line.type.raguly)

(defn pattern
  {:display-name "Raguly"
   :value        :raguly}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [half-width    (/ width 2)
        quarter-width (/ width 4)
        height        (* half-width height)
        dx            (-> width
                          (/ 2)
                          (* (-> eccentricity
                                 (* 0.7)
                                 (+ 0.3))))]
    ["l"
     [quarter-width 0]
     [(- dx) (- height)]
     [half-width 0]
     [dx height]
     [quarter-width 0]]))
