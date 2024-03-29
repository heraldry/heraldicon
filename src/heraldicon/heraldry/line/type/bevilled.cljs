(ns heraldicon.heraldry.line.type.bevilled)

(def pattern
  {:display-name :string.line.type/bevilled
   :full? true
   :function (fn [{:keys [height width
                          eccentricity] :as _line-data}
                  length
                  {:keys [real-start real-end] :as _line-options}]
               (let [real-start (or real-start 0)
                     real-end (or real-end length)
                     relevant-length (- real-end real-start)
                     half-width (/ width 2)
                     height (* half-width height)
                     pos-x (-> relevant-length
                               (- width)
                               (* eccentricity)
                               (+ half-width))]
                 {:pattern ["h" real-start
                            "h" (+ pos-x
                                   half-width)
                            "l" [(- width) height]
                            "h" (-> relevant-length
                                    (- pos-x)
                                    (+ half-width))
                            "h" (- length real-end)
                            "v" (- height)]
                  :min 0
                  :max height}))})
