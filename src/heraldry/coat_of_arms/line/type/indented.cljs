(ns heraldry.coat-of-arms.line.type.indented)

(defn pattern
  {:display-name "Indented"
   :value        :indented}
  [{:keys [height
           width]}
   _fimbriation-offset
   _line-options]
  (let [half-width (/ width 2)
        height     (* half-width height)]
    ["l"
     [half-width (- height)]
     [half-width height]]))
