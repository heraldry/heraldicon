(ns heraldicon.heraldry.line.type.wavy)

(def pattern
  {:display-name :string.line.type/wavy
   :function (fn [{:keys [eccentricity
                          height
                          width]}
                  _line-options]
               (let [radius-x (-> width
                                  (/ 4)
                                  (* (-> eccentricity
                                         (min 1)
                                         (* -0.5)
                                         (+ 1.5))))
                     radius-y (* radius-x height)
                     tx (-> width
                            (/ 2))
                     arc-height (-> (- 1 (/ (* (/ tx 2) (/ tx 2))
                                            (* radius-x radius-x)))
                                    Math/sqrt
                                    (* radius-y)
                                    (->> (- radius-y)))]
                 {:pattern ["a" radius-x radius-y 0 0 1 [tx 0]
                            "a" radius-x radius-y 0 0 0 [tx 0]]
                  :min (- arc-height)
                  :max arc-height}))})
