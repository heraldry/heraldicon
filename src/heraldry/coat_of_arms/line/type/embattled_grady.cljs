(ns heraldry.coat-of-arms.line.type.embattled-grady)

(def pattern
  {:display-name :string.line.type/embattled-grady
   :function (fn [{:keys [height
                          width]}
                  _line-options]
               (let [dx (/ width 4)
                     dy (* dx height)]
                 {:pattern ["l"
                            [(/ dx 2) 0]
                            [0 (- dy)]
                            [dx 0]
                            [0 (- dy)]
                            [dx 0]
                            [0 dy]
                            [dx 0]
                            [0 dy]
                            [(/ dx 2) 0]]
                  :min (+ (- dy)
                          (- dy))
                  :max 0}))})
