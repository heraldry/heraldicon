(ns heraldry.coat-of-arms.line.type.raguly)

(defn pattern
  {:display-name "Raguly"
   :value :raguly}
  [{:keys [eccentricity
           height
           width]}
   fimbriation-offset
   {:keys [reversed?] :as _line-options}]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        height (* half-width height)
        dx (-> width
               (/ 2)
               (* (-> eccentricity
                      (* 0.7)
                      (+ 0.3))))
        fimbriation-offset (-> fimbriation-offset
                               (min quarter-width)
                               (max (- quarter-width)))
        shift-x (-> fimbriation-offset
                    (* dx)
                    (/ height))]
    (if reversed?
      ["l"
       [(+ quarter-width
           fimbriation-offset
           (- shift-x)) 0]
       [dx (- height)]
       [(+ half-width
           (* -2 fimbriation-offset)) 0]
       [(- dx) height]
       [(+ quarter-width
           fimbriation-offset
           shift-x) 0]]
      ["l"
       [(+ quarter-width
           fimbriation-offset
           shift-x) 0]
       [(- dx) (- height)]
       [(+ half-width
           (* -2 fimbriation-offset)) 0]
       [dx height]
       [(+ quarter-width
           fimbriation-offset
           (- shift-x)) 0]])))
