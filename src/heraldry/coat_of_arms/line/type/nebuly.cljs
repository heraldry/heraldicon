(ns heraldry.coat-of-arms.line.type.nebuly)

(def pattern
  {:display-name :string.line.type/nebuly
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     dx (-> half-width
                            (* (-> eccentricity
                                   (min 1)
                                   (* 1.5))))

                     orientation-height (* half-width height)]
                 {:pattern ["c" (- dx) (- orientation-height) (+ half-width dx) (- orientation-height) half-width 0
                            "c" (- dx) orientation-height (+ half-width dx) orientation-height half-width 0]
                  :min (* 0.75 (- orientation-height)) ; should be the maximum point at t = 0.5
                  :max (* 0.75 orientation-height) ; should be the maximum point at t = 0.5
                  }))})
