(ns heraldry.coat-of-arms.line.type.straight)

(defn full
  {:display-name {:en "Straight"
                  :de "Gerade"}
   :value :straight
   :full? true}
  [{:keys [width]}
   _line-options]
  {:pattern ["h" width]
   :min 0
   :max 0})
