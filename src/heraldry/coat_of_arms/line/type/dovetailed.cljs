(ns heraldry.coat-of-arms.line.type.dovetailed)

(defn pattern
  {:display-name "Dovetailed"
   :value        :dovetailed}
  [{:keys [eccentricity
           height
           width]}
   fimbriation-offset
   _line-options]
  (let [half-width         (/ width 2)
        quarter-width      (/ width 4)
        height             (* half-width height)
        dx                 (-> width
                               (/ 4)
                               (* (-> eccentricity
                                      (* 0.5)
                                      (+ 0.2))))
        max-fo             (/ (- quarter-width dx)
                              (+ 1 (/ (* 2 dx) height)))
        fimbriation-offset (-> fimbriation-offset
                               (min max-fo)
                               (max (- max-fo)))
        shift-x            (-> fimbriation-offset
                               (* dx 2)
                               (/ height))]
    ["l"
     [(+ quarter-width
         dx
         fimbriation-offset
         shift-x) 0]
     [(* dx -2) (- height)]
     [(+ half-width
         dx
         dx
         (* -2 fimbriation-offset)
         (* -2 shift-x)) 0]
     [(* dx -2) height]
     [(+ quarter-width
         dx
         fimbriation-offset
         shift-x) 0]]))
