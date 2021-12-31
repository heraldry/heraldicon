(ns heraldry.coat-of-arms.line.type.indented)

(def pattern
  {:display-name :string.line.type/indented
   :function (fn [{:keys [height
                          width]}
                  _line-options]
               (let [half-width (/ width 2)
                     height (* half-width height)]
                 {:pattern ["l"
                            [half-width (- height)]
                            [half-width height]]
                  :min (- height)
                  :max 0}))})
