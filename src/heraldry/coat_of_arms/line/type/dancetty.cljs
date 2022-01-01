(ns heraldry.coat-of-arms.line.type.dancetty)

(def pattern
  {:display-name :string.line.type/dancetty
   :function (fn [{:keys [height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     quarter-width (/ width 4)
                     half-height (* quarter-width height)
                     height (* half-height 2)]
                 {:pattern ["l"
                            [quarter-width (- half-height)]
                            [half-width height]
                            [quarter-width (- half-height)]]
                  :min (- half-height)
                  :max half-height}))})
