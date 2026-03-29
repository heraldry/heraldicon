(ns heraldicon.heraldry.line.type.epine)

(def pattern
  {:display-name :string.line.type/epine
   :function (fn [{:keys [height width]}
                  _line-options]
               (let [sw (* width 0.08)
                     slope-w (- (/ width 2) (* 2 sw))
                     h (* (/ width 4) height)
                     sh (* h 0.25)]
                 {:pattern ["l"
                            slope-w (- h)
                            sw sh
                            sw (- sh)
                            slope-w h
                            sw sh
                            sw (- sh)]
                  :min (- h)
                  :max sh}))})
