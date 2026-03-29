(ns heraldicon.heraldry.line.type.bastionne)

(def pattern
  {:display-name :string.line.type/bastionne
   :function (fn [{:keys [height width]}
                  _line-options]
               ;; At height=1: tip angle = 90°, valley angle = 90°,
               ;; notch angles ≈ 98°. Peak and valley are identical shapes.
               (let [h (* width height)
                     slope-w (* width 0.3575)
                     slope-h (* h 0.3575)
                     notch-w (* width 0.215)
                     notch-h (* h 0.285)]
                 {:pattern ["l"
                            slope-w (- slope-h)
                            (- notch-w) (- notch-h)
                            slope-w (- slope-h)
                            slope-w slope-h
                            (- notch-w) notch-h
                            slope-w slope-h]
                  :min (- h)
                  :max 0}))})
