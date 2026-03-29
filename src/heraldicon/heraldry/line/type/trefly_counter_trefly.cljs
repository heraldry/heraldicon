(ns heraldicon.heraldry.line.type.trefly-counter-trefly)

(def pattern
  {:display-name :string.line.type/trefly-counter-trefly
   :function (fn [{:keys [height width]}
                  _line-options]
               (let [half-w (/ width 2)
                     r (* half-w 0.15 height)
                     stem-h r
                     gap (* r 0.5)
                     side-offset (* r 1.2)
                     extent (+ stem-h gap (* 2 r))]
                 {:pattern [;; advance to up-trefoil
                            "l" (/ half-w 2) 0
                            ;; stem up
                            0 (- stem-h)
                            ;; left lobe
                            (- side-offset) 0
                            "a" r r 0 0 1 [0 (- (* 2 r))]
                            "a" r r 0 0 1 [0 (* 2 r)]
                            "l" side-offset 0
                            ;; top lobe
                            0 (- gap)
                            "a" r r 0 0 1 [0 (- (* 2 r))]
                            "a" r r 0 0 1 [0 (* 2 r)]
                            "l" 0 gap
                            ;; right lobe
                            side-offset 0
                            "a" r r 0 0 1 [0 (- (* 2 r))]
                            "a" r r 0 0 1 [0 (* 2 r)]
                            "l" (- side-offset) 0
                            ;; stem down
                            0 stem-h
                            ;; advance to down-trefoil
                            (/ half-w 2) 0
                            ;; stem down
                            0 stem-h
                            ;; left lobe (downward)
                            (- side-offset) 0
                            "a" r r 0 0 0 [0 (* 2 r)]
                            "a" r r 0 0 0 [0 (- (* 2 r))]
                            "l" side-offset 0
                            ;; bottom lobe
                            0 gap
                            "a" r r 0 0 0 [0 (* 2 r)]
                            "a" r r 0 0 0 [0 (- (* 2 r))]
                            "l" 0 (- gap)
                            ;; right lobe (downward)
                            side-offset 0
                            "a" r r 0 0 0 [0 (* 2 r)]
                            "a" r r 0 0 0 [0 (- (* 2 r))]
                            "l" (- side-offset) 0
                            ;; stem up
                            0 (- stem-h)]
                  :min (- extent)
                  :max extent}))})
