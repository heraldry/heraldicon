(ns heraldry.coat-of-arms.line.type.dancetty)

(defn pattern
  {:display-name "Dancetty"
   :value :dancetty}
  [{:keys [height
           width]}
   _fimbriation-offset
   {:keys [reversed?] :as _line-options}]
  (let [half-width (/ width 2)
        quarter-width (/ width 4)
        half-height (* quarter-width height)
        height (* half-height 2)]
    (if reversed?
      ["l"
       [quarter-width half-height]
       [half-width (- height)]
       [quarter-width half-height]]
      ["l"
       [quarter-width (- half-height)]
       [half-width height]
       [quarter-width (- half-height)]])))
