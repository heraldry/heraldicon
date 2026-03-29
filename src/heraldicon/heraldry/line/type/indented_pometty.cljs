(ns heraldicon.heraldry.line.type.indented-pometty)

(def pattern
  {:display-name :string.line.type/indented-pometty
   :function (fn [{:keys [height width]}
                  _line-options]
               (let [quarter-width (/ width 4)
                     h (* (/ width 2) height)
                     r (* quarter-width 0.3)
                     l (Math/sqrt (+ (* quarter-width quarter-width) (* h h)))
                     ratio (/ r l)
                     diag-dx (* quarter-width (- 1 (* 2 ratio)))
                     diag-dy (* h (- 1 (* 2 ratio)))
                     arc-dx (* 2 r (/ quarter-width l))
                     half-arc-dx (/ arc-dx 2)
                     arc-mid-dy (* r (+ 1 (/ h l)))]
                 {:pattern [;; second half of valley arc (valley bottom -> departure)
                            "a" r r 0 0 0 [half-arc-dx (- arc-mid-dy)]
                            ;; diagonal up to peak approach
                            "l" diag-dx (- diag-dy)
                            ;; full peak arc
                            "a" r r 0 1 1 [arc-dx 0]
                            ;; diagonal down to valley approach
                            "l" diag-dx diag-dy
                            ;; full valley arc
                            "a" r r 0 1 0 [arc-dx 0]
                            ;; diagonal up to peak approach
                            "l" diag-dx (- diag-dy)
                            ;; full peak arc
                            "a" r r 0 1 1 [arc-dx 0]
                            ;; diagonal down to valley approach
                            "l" diag-dx diag-dy
                            ;; first half of valley arc (approach -> valley bottom)
                            "a" r r 0 0 0 [half-arc-dx arc-mid-dy]]
                  :min (- (+ h (* 2 r)))
                  :max 0}))})
