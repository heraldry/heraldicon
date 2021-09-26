(ns heraldry.coat-of-arms.line.type.dovetailed)

(defn pattern
  {:display-name {:en "Dovetailed"
                  :de "Schwalbenschwanzschnitt"}
   :value :dovetailed}
  [{:keys [eccentricity
           height
           width]}
   _line-options]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)
        dx (-> width
               (/ 4)
               (* (-> eccentricity
                      (* 0.5)
                      (+ 0.2))))]

    {:pattern ["l"
               [(+ quarter-width
                   dx) 0]
               [(* dx -2) (- height)]
               [(+ half-width
                   dx
                   dx) 0]
               [(* dx -2) height]
               [(+ quarter-width
                   dx) 0]]
     :min (- height)
     :max 0}))
