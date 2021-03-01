(ns heraldry.coat-of-arms.line.type.embattled)

(defn pattern
  {:display-name "Embattled"
   :value        :embattled}
  [{:keys [height
           width]}
   fimbriation-offset
   _line-options]
  (let [half-width         (/ width 2)
        quarter-width      (/ width 4)
        height             (* half-width height)
        fimbriation-offset (-> fimbriation-offset
                               (min quarter-width)
                               (max (- quarter-width)))]
    ["l"
     [(+ quarter-width
         fimbriation-offset) 0]
     [0 (- height)]
     [(- half-width
         (* 2 fimbriation-offset)) 0]
     [0 height]
     [(+ quarter-width
         fimbriation-offset) 0]]))
