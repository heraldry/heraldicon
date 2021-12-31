(ns heraldry.coat-of-arms.line.type.engrailed)

(def pattern
  {:display-name :string.line.type/engrailed
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [radius-x (-> width
                                  (/ 2)
                                  (* (-> eccentricity
                                         (min 1)
                                         (* -0.5)
                                         (+ 1.5))))
                     radius-y (* radius-x height)
                     tx (-> width
                            (/ 2))
                     ty (-> (- 1 (/ (* tx tx)
                                    (* radius-x radius-x)))
                            Math/sqrt
                            (* radius-y)
                            (->> (- radius-y)))]
                 {:pattern ["a" radius-x radius-y 0 0 0 [tx (- ty)]
                            "a" radius-x radius-y 0 0 0 [tx ty]]
                  :min (- ty)
                  :max 0}))})
