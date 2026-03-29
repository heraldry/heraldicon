(ns heraldicon.heraldry.line.type.palissado)

(def pattern
  {:display-name :string.line.type/palissado
   :function (fn [{:keys [height width]}
                  _line-options]
               (let [h (* width height)
                     base-w (/ width 3)
                     stake-w (/ width 6)
                     peak-w (/ width 3)
                     base-h (/ h 4)
                     stake-h (/ h 2)
                     peak-h (- h base-h stake-h)]
                 {:pattern ["l"
                            base-w (- base-h)
                            (- stake-w) (- stake-h)
                            peak-w (- peak-h)
                            peak-w peak-h
                            (- stake-w) stake-h
                            base-w base-h]
                  :min (- h)
                  :max 0}))})
