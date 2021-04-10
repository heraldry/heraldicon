(ns heraldry.coat-of-arms.line.type.straight)

(defn pattern
  {:display-name "Straight"
   :value :straight}
  [{:keys [width]}
   _line-options]
  {:pattern ["h" width]
   :min 0
   :max 0})
