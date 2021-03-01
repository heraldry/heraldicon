(ns heraldry.coat-of-arms.line.type.invected)

(defn pattern
  {:display-name "Invected"
   :value        :invected}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   _line-options]
  (let [radius-x (-> width
                     (/ 2)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)]
    ["a" radius-x radius-y 0 0 1 [width 0]]))
