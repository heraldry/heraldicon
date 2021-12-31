(ns heraldry.coat-of-arms.line.type.straight)

(def pattern
  {:display-name :string.line.type/straight
   :full? true
   :function (fn [{:keys [width]}
                  _line-options]
               {:pattern ["h" width]
                :min 0
                :max 0})})
