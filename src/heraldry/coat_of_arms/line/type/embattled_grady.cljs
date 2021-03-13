(ns heraldry.coat-of-arms.line.type.embattled-grady)

(defn pattern
  {:display-name "Embattled grady"
   :value :embattled-grady}
  [{:keys [height
           width]}
   _line-options]
  (let [dx (/ width 4)
        dy (* dx height)]
    ["l"
     [(/ dx 2) 0]
     [0 (- dy)]
     [dx 0]
     [0 dy]
     [dx 0]
     [0 dy]
     [dx 0]
     [0 (- dy)]
     [(/ dx 2) 0]]))
