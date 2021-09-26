(ns heraldry.coat-of-arms.line.type.embattled-grady)

(defn pattern
  {:display-name {:en "Embattled grady"
                  :de "Stufengiebelschnitt"}
   :value :embattled-grady}
  [{:keys [height
           width]}
   _line-options]
  (let [dx (/ width 4)
        dy (* dx height)]
    {:pattern ["l"
               [(/ dx 2) 0]
               [0 (- dy)]
               [dx 0]
               [0 (- dy)]
               [dx 0]
               [0 dy]
               [dx 0]
               [0 dy]
               [(/ dx 2) 0]]
     :min (+ (- dy)
             (- dy))
     :max 0}))
