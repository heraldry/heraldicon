(ns heraldicon.heraldry.line.type.indented)

(def pattern
  {:display-name :string.line.type/indented
   :function (fn [{:keys [height
                          width]}
                  _line-options]
               (let [quarter-width (/ width 4)
                     height (* quarter-width height)]
                 {:pattern ["l"
                            quarter-width (- height)
                            quarter-width height
                            quarter-width (- height)
                            quarter-width height]
                  :min (- height)
                  :max 0}))})
