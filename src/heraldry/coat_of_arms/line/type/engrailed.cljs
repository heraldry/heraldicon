(ns heraldry.coat-of-arms.line.type.engrailed)

(defn pattern
  {:display-name "Engrailed"
   :value        :engrailed}
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
        radius-y (* radius-x height)
        tx       (-> width
                     (/ 2))
        ty       (-> (- 1 (/ (* tx tx)
                             (* radius-x radius-x)))
                     Math/sqrt
                     (* radius-y)
                     (->> (- radius-y)))]
    ["a" radius-x radius-y 0 0 0 [tx (- ty)]
     "a" radius-x radius-y 0 0 0 [tx ty]]))
