(ns heraldry.coat-of-arms.line.type.straight)

(defn pattern
  {:display-name "Straight"
   :value        :straight}
  [{:keys [width]}
   _line-options]
  ["h" width])
