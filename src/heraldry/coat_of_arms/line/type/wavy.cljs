(ns heraldry.coat-of-arms.line.type.wavy)

(defn pattern
  {:display-name "Wavy / undy"
   :value        :wavy}
  [{:keys [eccentricity
           height
           width]}
   _fimbriation-offset
   {:keys [reversed?] :as _line-options}]
  (let [radius-x (-> width
                     (/ 4)
                     (* (-> eccentricity
                            (min 1)
                            (* -0.5)
                            (+ 1.5))))
        radius-y (* radius-x height)
        tx       (-> width
                     (/ 2))]
    ["a" radius-x radius-y 0 0 (if reversed? 0 1) [tx 0]
     "a" radius-x radius-y 0 0 (if reversed? 1 0) [tx 0]]))
