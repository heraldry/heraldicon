(ns heraldry.coat-of-arms.line.type.invected)

(defn pattern
  {:display-name {:en "Invected"
                  :de "Lappenschnitt"}
   :value :invected}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [tx (-> width
               (/ 2))
        radius-x (-> tx
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        arc-height (-> (- 1 (/ (* tx tx)
                               (* radius-x radius-x)))
                       Math/sqrt
                       (* radius-y)
                       (->> (- radius-y)))]
    {:pattern ["a" radius-x radius-y 0 0 1 [width 0]]
     :min (- arc-height)
     :max 0}))
