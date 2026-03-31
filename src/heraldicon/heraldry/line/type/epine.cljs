(ns heraldicon.heraldry.line.type.epine)

(def pattern
  {:display-name :string.line.type/epine
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  {:keys [reversed?]}]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     height (* half-width height)
                     half-height (/ height 2)
                     dip (-> height
                             (* (-> eccentricity
                                    (* 0.5)
                                    (+ 0.2))))
                     dx (-> width
                            (/ 4)
                            (* (-> eccentricity
                                   (* 0.5)
                                   (+ 0.2))))]
                 {:pattern (if reversed?
                             ["l"
                              (- dx) half-height
                              (+ quarter-width dx) (- dip)
                              (+ quarter-width dx) dip
                              (* dx -2) (- height)
                              (+ quarter-width dx) dip
                              (+ quarter-width dx) (- dip)
                              (- dx) half-height]
                             ["l"
                              (- dx) (- half-height)
                              (+ quarter-width dx) dip
                              (+ quarter-width dx) (- dip)
                              (* dx -2) height
                              (+ quarter-width dx) (- dip)
                              (+ quarter-width dx) dip
                              (- dx) (- half-height)])
                  :min (- half-height)
                  :max half-height
                  :additional-offset (/ quarter-width
                                        width)}))})
