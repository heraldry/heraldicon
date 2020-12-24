(ns or.coad.division
  (:require [or.coad.field-environment :as field-environment]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn get-field [fields index]
  (let [part (get fields index)]
    (if (number? part)
      (get fields part)
      part)))

(defn per-pale [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1     (svg/id "division-pale-1")
        mask-id-2     (svg/id "division-pale-2")
        top-left      (get-in environment [:points :top-left])
        bottom-right  (get-in environment [:points :bottom-right])
        top           (get-in environment [:points :top])
        bottom        (get-in environment [:points :bottom])
        line-style    (or (:style line) :straight)
        {line :line}  (line/create line-style
                                   (:y (v/- bottom top))
                                   :angle -90)
        environment-1 (field-environment/create
                       (svg/make-path ["M" bottom
                                       (line/stitch line)
                                       (infinity/path :counter-clockwise
                                                      [:top :bottom]
                                                      [top bottom])
                                       "z"])
                       {:parent       field
                        :context      [:per-pale :left]
                        :bounding-box (svg/bounding-box
                                       [top-left bottom])})
        environment-2 (field-environment/create
                       (svg/make-path ["M" bottom
                                       (line/stitch line)
                                       (infinity/path :clockwise
                                                      [:top :bottom]
                                                      [top bottom])
                                       "z"])
                       {:parent       field
                        :context      [:per-pale :left]
                        :bounding-box (svg/bounding-box
                                       [top bottom-right])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bottom
                     (line/stitch line)])}]])]))

(defn per-fess [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1     (svg/id "division-fess-1")
        mask-id-2     (svg/id "division-fess-2")
        top-left      (get-in environment [:points :top-left])
        bottom-right  (get-in environment [:points :bottom-right])
        left          (get-in environment [:points :left])
        right         (get-in environment [:points :right])
        line-style    (or (:style line) :straight)
        {line :line}  (line/create line-style
                                   (:x (v/- right left)))
        environment-1 (field-environment/create
                       (svg/make-path ["M" left
                                       (line/stitch line)
                                       (infinity/path :counter-clockwise
                                                      [:right :left]
                                                      [right left])
                                       "z"])
                       {:parent       field
                        :context      [:per-fess :top]
                        :bounding-box (svg/bounding-box
                                       [top-left right])})
        environment-2 (field-environment/create
                       (svg/make-path ["M" left
                                       (line/stitch line)
                                       (infinity/path :clockwise
                                                      [:right :left]
                                                      [right left])
                                       "z"])
                       {:parent       field
                        :context      [:per-fess :bottom]
                        :bounding-box (svg/bounding-box
                                       [left bottom-right])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" left
                     (line/stitch line)])}]])]))

(defn per-bend [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1         (svg/id "division-bend-1")
        mask-id-2         (svg/id "division-bend-2")
        top-left          (get-in environment [:points :top-left])
        top-right         (get-in environment [:points :top-right])
        fess              (get-in environment [:points :fess])
        bend-intersection (v/project top-left fess (:x top-right))
        line-style        (or (:style line) :straight)
        {line :line}      (line/create line-style
                                       (v/abs (v/- bend-intersection top-left))
                                       :angle 45)
        environment-1     (field-environment/create
                           (svg/make-path ["M" top-left
                                           (line/stitch line)
                                           (infinity/path :counter-clockwise
                                                          [:right :top-left]
                                                          [bend-intersection top-left])
                                           "z"])
                           {:parent       field
                            :context      [:per-bend :top]
                            :bounding-box (svg/bounding-box
                                           [top-left bend-intersection])})
        environment-2     (field-environment/create
                           (svg/make-path ["M" top-left
                                           (line/stitch line)
                                           (infinity/path :clockwise
                                                          [:right :top-left]
                                                          [bend-intersection top-left])
                                           "z"])
                           {:parent       field
                            :context      [:per-bend :bottom]
                            :bounding-box (svg/bounding-box
                                           [top-left bend-intersection])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-left
                     (line/stitch line)])}]])]))

(defn per-bend-sinister [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                  (svg/id "division-bend-right-1")
        mask-id-2                  (svg/id "division-bend-right-2")
        top-left                   (get-in environment [:points :top-left])
        top-right                  (get-in environment [:points :top-right])
        bottom-left                (get-in environment [:points :bottom-left])
        fess                       (get-in environment [:points :fess])
        bend-intersection          (v/project top-right fess (:x top-left))
        line-style                 (or (:style line) :straight)
        {line        :line
         line-length :length}      (line/create line-style
                                                (v/abs (v/- bend-intersection top-right))
                                                :angle -45
                                                :flipped? false)
        bend-intersection-adjusted (v/extend
                                       top-right
                                     bend-intersection
                                     line-length)
        environment-1              (field-environment/create
                                    (svg/make-path ["M" bend-intersection-adjusted
                                                    (line/stitch line)
                                                    (infinity/path :counter-clockwise
                                                                   [:top-right :left]
                                                                   [top-right bend-intersection])
                                                    "z"])
                                    {:parent       field
                                     :context      [:per-bend-sinister :top]
                                     :bounding-box (svg/bounding-box
                                                    [top-right bend-intersection])})
        environment-2              (field-environment/create
                                    (svg/make-path ["M" bend-intersection-adjusted
                                                    (line/stitch line)
                                                    (infinity/path :clockwise
                                                                   [:top-right :left]
                                                                   [top-right bend-intersection])
                                                    "z"])
                                    {:parent       field
                                     :context      [:per-bend-sinister :bottom]
                                     :bounding-box (svg/bounding-box
                                                    [top-right bottom-left])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bend-intersection-adjusted
                     (line/stitch line)])}]])]))

(defn per-chevron [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                       (svg/id "division-chevron-1")
        mask-id-2                       (svg/id "division-chevron-2")
        line-style                      (or (:style line) :straight)
        top-left                        (get-in environment [:points :top-left])
        top-right                       (get-in environment [:points :top-right])
        bottom-left                     (get-in environment [:points :bottom-left])
        bottom-right                    (get-in environment [:points :bottom-right])
        fess                            (get-in environment [:points :fess])
        bend-intersection-left          (v/project top-right fess (:x top-left))
        bend-intersection-right         (v/project top-left fess (:x top-right))
        {line-left        :line
         line-left-length :length}      (line/create line-style
                                                     (v/abs (v/- bend-intersection-left fess))
                                                     :angle -45)
        {line-right :line}              (line/create line-style
                                                     (v/abs (v/- bend-intersection-right fess))
                                                     :angle 45)
        bend-intersection-left-adjusted (v/extend fess bend-intersection-left line-left-length)
        environment-1                   (field-environment/create
                                         (svg/make-path ["M" bend-intersection-left-adjusted
                                                         (line/stitch line-left)
                                                         "L" fess
                                                         (line/stitch line-right)
                                                         (infinity/path :counter-clockwise
                                                                        [:right :left]
                                                                        [bend-intersection-right bend-intersection-left])
                                                         "z"])
                                         {:parent       field
                                          :context      [:per-chevron :top]
                                          :bounding-box (svg/bounding-box
                                                         [top-left bottom-right])})
        environment-2                   (field-environment/create
                                         (svg/make-path ["M" bend-intersection-left-adjusted
                                                         (line/stitch line-left)
                                                         "L" fess
                                                         (line/stitch line-right)
                                                         (infinity/path :clockwise
                                                                        [:right :left]
                                                                        [bend-intersection-right bend-intersection-left])
                                                         "z"
                                                         "z"])
                                         {:parent       field
                                          :context      [:per-chevron :bottom]
                                          :bounding-box (svg/bounding-box
                                                         [bottom-left fess bottom-right])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bend-intersection-left-adjusted
                     (line/stitch line-left)
                     "L" fess
                     (line/stitch line-right)])}]])]))

(defn per-saltire [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                          (svg/id "division-saltire-1")
        mask-id-2                          (svg/id "division-saltire-2")
        mask-id-3                          (svg/id "division-saltire-3")
        mask-id-4                          (svg/id "division-saltire-4")
        line-style                         (or (:style line) :straight)
        top-left                           (get-in environment [:points :top-left])
        top-right                          (get-in environment [:points :top-right])
        bottom-left                        (get-in environment [:points :bottom-left])
        bottom-right                       (get-in environment [:points :bottom-right])
        fess                               (get-in environment [:points :fess])
        bend-intersection-right            (v/project top-left fess (:x top-right))
        bend-intersection-left             (v/project top-right fess (:x top-left))
        {line-top-left        :line
         line-top-left-length :length}     (line/create line-style
                                                        (v/abs (v/- top-left fess))
                                                        :angle 45
                                                        :reversed? true)
        {line-top-right :line}             (line/create line-style
                                                        (v/abs (v/- top-right fess))
                                                        :angle -45
                                                        :flipped? true)
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line-style
                                                        (v/abs (v/- bend-intersection-right fess))
                                                        :angle 225
                                                        :reversed? true)
        {line-bottom-left :line}           (line/create line-style
                                                        (v/abs (v/- bend-intersection-left fess))
                                                        :angle -225
                                                        :flipped? true)
        top-left-adjusted                  (v/extend
                                               fess
                                             top-left
                                             line-top-left-length)
        bend-intersection-right-adjusted   (v/extend
                                               fess
                                             bend-intersection-right
                                             line-bottom-right-length)
        environment-1                      (field-environment/create
                                            (svg/make-path ["M" top-left-adjusted
                                                            (line/stitch line-top-left)
                                                            "L" fess
                                                            (line/stitch line-top-right)
                                                            (infinity/path :counter-clockwise
                                                                           [:top-right :top-left]
                                                                           [top-right top-left])
                                                            "z"])
                                            {:parent       field
                                             :context      [:per-saltire :top]
                                             :bounding-box (svg/bounding-box
                                                            [top-left fess top-right])})
        environment-2                      (field-environment/create
                                            (svg/make-path ["M" bend-intersection-right-adjusted
                                                            (line/stitch line-bottom-right)
                                                            "L" fess
                                                            (line/stitch line-top-right)
                                                            (infinity/path :clockwise
                                                                           [:top-right :bottom]
                                                                           [top-right bend-intersection-right])
                                                            "z"])
                                            {:parent       field
                                             :context      [:per-saltire :right]
                                             :bounding-box (svg/bounding-box
                                                            [top-right fess bottom-right])})
        environment-3                      (field-environment/create
                                            (svg/make-path ["M" bend-intersection-right-adjusted
                                                            (line/stitch line-bottom-right)
                                                            "L" fess
                                                            (line/stitch line-bottom-left)
                                                            (infinity/path :counter-clockwise
                                                                           [:left :right]
                                                                           [bend-intersection-left bend-intersection-right])
                                                            "z"])
                                            {:parent       field
                                             :context      [:per-saltire :bottom]
                                             :bounding-box (svg/bounding-box
                                                            [bottom-left fess bottom-right])})
        environment-4                      (field-environment/create
                                            (svg/make-path ["M" top-left-adjusted
                                                            (line/stitch line-top-left)
                                                            "L" fess
                                                            (line/stitch line-bottom-left)
                                                            (infinity/path :clockwise
                                                                           [:bottom-left :top-left]
                                                                           [bend-intersection-left top-left])
                                                            "z"])
                                            {:parent       field
                                             :context      [:per-saltire :left]
                                             :bounding-box (svg/bounding-box
                                                            [top-left fess bottom-left])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" bend-intersection-right-adjusted
                            (line/stitch line-bottom-right)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" fess
                            (line/stitch line-bottom-left)])}]]
      [:mask {:id mask-id-4}
       [:path {:d    (:shape environment-4)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get-field fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get-field fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get-field fields 2) environment-3 options]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (get-field fields 3) environment-4 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" bend-intersection-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-bottom-left)])}]])]))

(defn quarterly [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                    (svg/id "division-quarterly-1")
        mask-id-2                    (svg/id "division-quarterly-2")
        mask-id-3                    (svg/id "division-quarterly-3")
        mask-id-4                    (svg/id "division-quarterly-4")
        line-style                   (or (:style line) :straight)
        top-left                     (get-in environment [:points :top-left])
        top-right                    (get-in environment [:points :top-right])
        bottom-left                  (get-in environment [:points :bottom-left])
        bottom-right                 (get-in environment [:points :bottom-right])
        top                          (get-in environment [:points :top])
        bottom                       (get-in environment [:points :bottom])
        fess                         (get-in environment [:points :fess])
        left                         (get-in environment [:points :left])
        right                        (get-in environment [:points :right])
        {line-top        :line
         line-top-length :length}    (line/create line-style
                                                  (v/abs (v/- top fess))
                                                  :angle 90
                                                  :reversed? true)
        {line-right :line}           (line/create line-style
                                                  (v/abs (v/- right fess))
                                                  :flipped? true)
        {line-bottom        :line
         line-bottom-length :length} (line/create line-style
                                                  (v/abs (v/- bottom fess))
                                                  :angle -90
                                                  :reversed? true)
        {line-left :line}            (line/create line-style
                                                  (v/abs (v/- left fess))
                                                  :angle -180
                                                  :flipped? true)
        top-adjusted                 (v/extend fess top line-top-length)
        bottom-adjusted              (v/extend fess bottom line-bottom-length)
        environment-1                (field-environment/create
                                      (svg/make-path ["M" top-adjusted
                                                      (line/stitch line-top)
                                                      "L" fess
                                                      (line/stitch line-left)
                                                      (infinity/path :clockwise
                                                                     [:left :top]
                                                                     [left top])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-quarterly :top-left]
                                       :bounding-box (svg/bounding-box
                                                      [top-left fess])})
        environment-2                (field-environment/create
                                      (svg/make-path ["M" top-adjusted
                                                      (line/stitch line-top)
                                                      "L" fess
                                                      (line/stitch line-right)
                                                      (infinity/path :counter-clockwise
                                                                     [:right :top]
                                                                     [right top])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-quarterly :top-right]
                                       :bounding-box (svg/bounding-box
                                                      [fess top-right])})
        environment-3                (field-environment/create
                                      (svg/make-path ["M" bottom-adjusted
                                                      (line/stitch line-bottom)
                                                      "L" fess
                                                      (line/stitch line-right)
                                                      (infinity/path :clockwise
                                                                     [:right :bottom]
                                                                     [right bottom])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-quarterly :bottom-right]
                                       :bounding-box (svg/bounding-box
                                                      [fess bottom-right])})
        environment-4                (field-environment/create
                                      (svg/make-path ["M" bottom-adjusted
                                                      (line/stitch line-bottom)
                                                      "L" fess
                                                      (line/stitch line-left)
                                                      (infinity/path :counter-clockwise
                                                                     [:left :bottom]
                                                                     [left bottom])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-quarterly :bottom-left]
                                       :bounding-box (svg/bounding-box
                                                      [fess bottom-left])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" fess
                            (line/stitch line-right)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" bottom-adjusted
                            (line/stitch line-bottom)])}]]
      [:mask {:id mask-id-4}
       [:path {:d    (:shape environment-4)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get-field fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get-field fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get-field fields 2) environment-3 options]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (get-field fields 3) environment-4 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-adjusted
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-left)])}]])]))

(defn gyronny [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                    (svg/id "division-gyronny-1")
        mask-id-2                    (svg/id "division-gyronny-2")
        mask-id-3                    (svg/id "division-gyronny-3")
        mask-id-4                    (svg/id "division-gyronny-4")
        mask-id-5                    (svg/id "division-gyronny-5")
        mask-id-6                    (svg/id "division-gyronny-6")
        mask-id-7                    (svg/id "division-gyronny-7")
        mask-id-8                    (svg/id "division-gyronny-8")
        line-style                   (or (:style line) :straight)
        top-left                     (get-in environment [:points :top-left])
        top-right                    (get-in environment [:points :top-right])
        bottom-left                  (get-in environment [:points :bottom-left])
        bottom-right                 (get-in environment [:points :bottom-right])
        top                          (get-in environment [:points :top])
        bottom                       (get-in environment [:points :bottom])
        fess                         (get-in environment [:points :fess])
        left                         (get-in environment [:points :left])
        right                        (get-in environment [:points :right])
        {line-top        :line
         line-top-length :length}    (line/create line-style
                                                  (v/abs (v/- top fess))
                                                  :angle 90
                                                  :reversed? true)
        {line-right        :line
         line-right-length :length}  (line/create line-style
                                                  (v/abs (v/- right fess))
                                                  :reversed? true
                                                  :angle 180)
        {line-bottom        :line
         line-bottom-length :length} (line/create line-style
                                                  (v/abs (v/- bottom fess))
                                                  :angle -90
                                                  :reversed? true)
        {line-left        :line
         line-left-length :length}   (line/create line-style
                                                  (v/abs (v/- left fess))
                                                  :reversed? true)
        top-adjusted                 (v/extend fess top line-top-length)
        bottom-adjusted              (v/extend fess bottom line-bottom-length)
        left-adjusted                (v/extend fess left line-left-length)
        right-adjusted               (v/extend fess right line-right-length)
        bend-intersection-left       (v/project top-right fess (:x top-left))
        bend-intersection-right      (v/project top-left fess (:x top-right))
        {line-top-left :line}        (line/create line-style
                                                  (v/abs (v/- top-left fess))
                                                  :flipped? true
                                                  :angle -135)
        {line-top-right :line}       (line/create line-style
                                                  (v/abs (v/- top-right fess))
                                                  :flipped? true
                                                  :angle -45)
        {line-bottom-right :line}    (line/create line-style
                                                  (v/abs (v/- bend-intersection-right fess))
                                                  :flipped? true
                                                  :angle 45)
        {line-bottom-left :line}     (line/create line-style
                                                  (v/abs (v/- bend-intersection-left fess))
                                                  :flipped? true
                                                  :angle -225)
        environment-1                (field-environment/create
                                      (svg/make-path ["M" top-adjusted
                                                      (line/stitch line-top)
                                                      "L" fess
                                                      (line/stitch line-top-left)
                                                      (infinity/path :clockwise
                                                                     [:top-left :top]
                                                                     [top-left top])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :one]
                                       :bounding-box (svg/bounding-box
                                                      [top-left fess top])})
        environment-2                (field-environment/create
                                      (svg/make-path ["M" top-adjusted
                                                      (line/stitch line-top)
                                                      "L" fess
                                                      (line/stitch line-top-right)
                                                      (infinity/path :counter-clockwise
                                                                     [:top-right :top]
                                                                     [top-right top])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :two]
                                       :bounding-box (svg/bounding-box
                                                      [top fess top-right])})
        environment-3                (field-environment/create
                                      (svg/make-path ["M" right-adjusted
                                                      (line/stitch line-right)
                                                      "L" fess
                                                      (line/stitch line-top-right)
                                                      (infinity/path :clockwise
                                                                     [:top-right :right]
                                                                     [top-right right])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :three]
                                       :bounding-box (svg/bounding-box
                                                      [top-right fess right])})
        environment-4                (field-environment/create
                                      (svg/make-path ["M" right-adjusted
                                                      (line/stitch line-right)
                                                      "L" fess
                                                      (line/stitch line-bottom-right)
                                                      (infinity/path :counter-clockwise
                                                                     [:bottom-right :right]
                                                                     [bend-intersection-right right])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :four]
                                       :bounding-box (svg/bounding-box
                                                      [right fess bottom-right])})
        environment-5                (field-environment/create
                                      (svg/make-path ["M" bottom-adjusted
                                                      (line/stitch line-bottom)
                                                      "L" fess
                                                      (line/stitch line-bottom-right)
                                                      (infinity/path :clockwise
                                                                     [:bottom-right :bottom]
                                                                     [bend-intersection-right bottom])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :five]
                                       :bounding-box (svg/bounding-box
                                                      [bottom-right fess bottom])})
        environment-6                (field-environment/create
                                      (svg/make-path ["M" bottom-adjusted
                                                      (line/stitch line-bottom)
                                                      "L" fess
                                                      (line/stitch line-bottom-left)
                                                      (infinity/path :counter-clockwise
                                                                     [:bottom-left :bottom]
                                                                     [bend-intersection-left bottom])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :six]
                                       :bounding-box (svg/bounding-box
                                                      [bottom fess bottom-left])})
        environment-7                (field-environment/create
                                      (svg/make-path ["M" left-adjusted
                                                      (line/stitch line-left)
                                                      "L" fess
                                                      (line/stitch line-bottom-left)
                                                      (infinity/path :clockwise
                                                                     [:bottom-left :left]
                                                                     [bend-intersection-left left])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :seven]
                                       :bounding-box (svg/bounding-box
                                                      [bottom-left fess left])})
        environment-8                (field-environment/create
                                      (svg/make-path ["M" left-adjusted
                                                      (line/stitch line-left)
                                                      "L" fess
                                                      (line/stitch line-top-left)
                                                      (infinity/path :counter-clockwise
                                                                     [:top-left :left]
                                                                     [top-left left])
                                                      "z"])
                                      {:parent       field
                                       :context      [:per-gyronny :eight]
                                       :bounding-box (svg/bounding-box
                                                      [left fess top-left])})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" fess
                            (line/stitch line-top-right)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" right-adjusted
                            (line/stitch line-right)])}]]
      [:mask {:id mask-id-4}
       [:path {:d    (:shape environment-4)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" fess
                            (line/stitch line-bottom-right)])}]]
      [:mask {:id mask-id-5}
       [:path {:d    (:shape environment-5)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" bottom-adjusted
                            (line/stitch line-bottom)])}]]
      [:mask {:id mask-id-6}
       [:path {:d    (:shape environment-6)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" fess
                            (line/stitch line-bottom-left)])}]]
      [:mask {:id mask-id-7}
       [:path {:d    (:shape environment-7)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" left-adjusted
                            (line/stitch line-left)])}]]
      [:mask {:id mask-id-8}
       [:path {:d    (:shape environment-8)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get-field fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get-field fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get-field fields 2) environment-3 options]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (get-field fields 3) environment-4 options]]
     [:g {:mask (str "url(#" mask-id-5 ")")}
      [top-level-render (get-field fields 4) environment-5 options]]
     [:g {:mask (str "url(#" mask-id-6 ")")}
      [top-level-render (get-field fields 5) environment-6 options]]
     [:g {:mask (str "url(#" mask-id-7 ")")}
      [top-level-render (get-field fields 6) environment-7 options]]
     [:g {:mask (str "url(#" mask-id-8 ")")}
      [top-level-render (get-field fields 7) environment-8 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" top-adjusted
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" right-adjusted
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" left-adjusted
                     (line/stitch line-left)])}]])]))

(defn tierced-per-pale [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                      (svg/id "division-tierced-pale-1")
        mask-id-2                      (svg/id "division-tierced-pale-2")
        mask-id-3                      (svg/id "division-tierced-pale-3")
        line-style                     (or (:style line) :straight)
        top                            (get-in environment [:points :top])
        bottom                         (get-in environment [:points :bottom])
        fess                           (get-in environment [:points :fess])
        width                          (:width environment)
        col1                           (- (:x fess) (/ width 6))
        col2                           (+ (:x fess) (/ width 6))
        first-top                      (v/v col1 (:y top))
        first-bottom                   (v/v col1 (:y bottom))
        second-top                     (v/v col2 (:y top))
        second-bottom                  (v/v col2 (:y bottom))
        {line :line}                   (line/create line-style
                                                    (:y (v/- bottom top))
                                                    :flipped? true
                                                    :angle 90)
        {line-reversed        :line
         line-reversed-length :length} (line/create line-style
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true)
        second-bottom-adjusted         (v/extend second-top second-bottom line-reversed-length)
        environment-1                  (field-environment/create
                                        (svg/make-path ["M" first-top
                                                        (line/stitch line)
                                                        (infinity/path :clockwise
                                                                       [:bottom :top]
                                                                       [first-bottom first-top])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-pale :left]})
        environment-2                  (field-environment/create
                                        (svg/make-path ["M" second-bottom-adjusted
                                                        (line/stitch line-reversed)
                                                        (infinity/path :counter-clockwise
                                                                       [:top :top]
                                                                       [second-top first-top])
                                                        (line/stitch line)
                                                        (infinity/path :counter-clockwise
                                                                       [:bottom :bottom]
                                                                       [first-bottom second-bottom])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-pale :middle]})
        environment-3                  (field-environment/create
                                        (svg/make-path ["M" second-bottom-adjusted
                                                        (line/stitch line-reversed)
                                                        (infinity/path :clockwise
                                                                       [:top :bottom]
                                                                       [second-top second-bottom])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-pale :right]})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" second-bottom-adjusted
                            (line/stitch line-reversed)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get fields 2) environment-3 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn tierced-per-fess [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                      (svg/id "division-tierced-fess-1")
        mask-id-2                      (svg/id "division-tierced-fess-2")
        mask-id-3                      (svg/id "division-tierced-fess-3")
        line-style                     (or (:style line) :straight)
        left                           (get-in environment [:points :left])
        right                          (get-in environment [:points :right])
        fess                           (get-in environment [:points :fess])
        height                         (:height environment)
        row1                           (- (:y fess) (/ height 6))
        row2                           (+ (:y fess) (/ height 6))
        first-left                     (v/v (:x left) row1)
        first-right                    (v/v (:x right) row1)
        second-left                    (v/v (:x left) row2)
        second-right                   (v/v (:x right) row2)
        {line :line}                   (line/create line-style
                                                    (:x (v/- right left)))
        {line-reversed        :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        environment-1                  (field-environment/create
                                        (svg/make-path ["M" first-left
                                                        (line/stitch line)
                                                        (infinity/path :counter-clockwise
                                                                       [:right :left]
                                                                       [first-right first-left])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-fess :top]})
        environment-2                  (field-environment/create
                                        (svg/make-path ["M" first-left
                                                        (line/stitch line)
                                                        (infinity/path :clockwise
                                                                       [:right :right]
                                                                       [first-right second-right-adjusted])
                                                        (line/stitch line-reversed)
                                                        (infinity/path :clockwise
                                                                       [:left :left]
                                                                       [second-left first-left])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-fess :middle]})
        environment-3                  (field-environment/create
                                        (svg/make-path ["M" second-right-adjusted
                                                        (line/stitch line-reversed)
                                                        (infinity/path :counter-clockwise
                                                                       [:left :right]
                                                                       [second-left second-right-adjusted])
                                                        "z"])
                                        {:parent  field
                                         :context [:tierced-per-fess :bottom]})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" second-right-adjusted
                            (line/stitch line-reversed)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get fields 2) environment-3 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn tierced-per-pairle [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                             (svg/id "division-tierced-pairle-1")
        mask-id-2                             (svg/id "division-tierced-pairle-2")
        mask-id-3                             (svg/id "division-tierced-pairle-3")
        line-style                            (or (:style line) :straight)
        top-left                              (get-in environment [:points :top-left])
        top-right                             (get-in environment [:points :top-right])
        bottom                                (get-in environment [:points :bottom])
        fess                                  (get-in environment [:points :fess])
        {line-top-left        :line
         line-top-left-length :length}        (line/create line-style
                                                           (v/abs (v/- top-left fess))
                                                           :angle 45
                                                           :reversed? true)
        {line-top-right :line}                (line/create line-style
                                                           (v/abs (v/- top-right fess))
                                                           :angle -45
                                                           :flipped? true)
        {line-bottom :line}                   (line/create line-style
                                                           (v/abs (v/- bottom fess))
                                                           :flipped? true
                                                           :angle 90)
        {line-bottom-reversed        :line
         line-bottom-reversed-length :length} (line/create line-style
                                                           (v/abs (v/- bottom fess))
                                                           :angle -90
                                                           :reversed? true)
        top-left-adjusted                     (v/extend
                                                  fess
                                                top-left
                                                line-top-left-length)
        bottom-adjusted                       (v/extend
                                                  fess
                                                bottom
                                                line-bottom-reversed-length)
        environment-1                         (field-environment/create
                                               (svg/make-path ["M" top-left-adjusted
                                                               (line/stitch line-top-left)
                                                               "L" fess
                                                               (line/stitch line-top-right)
                                                               (infinity/path :counter-clockwise
                                                                              [:top-right :top-left]
                                                                              [top-right top-left])
                                                               "z"])
                                               {:parent  field
                                                :context [:tierced-per-pairle :top]})
        environment-2                         (field-environment/create
                                               (svg/make-path ["M" bottom-adjusted
                                                               (line/stitch line-bottom-reversed)
                                                               "L" fess
                                                               (line/stitch line-top-right)
                                                               (infinity/path :clockwise
                                                                              [:top-right :bottom]
                                                                              [top-right bottom])
                                                               "z"])
                                               {:parent  field
                                                :context [:tierced-per-pairle :right]})
        environment-3                         (field-environment/create
                                               (svg/make-path ["M" top-left-adjusted
                                                               (line/stitch line-top-left)
                                                               "L" fess
                                                               (line/stitch line-bottom)
                                                               (infinity/path :clockwise
                                                                              [:bottom :top-left]
                                                                              [bottom top-left])
                                                               "z"])
                                               {:parent  field
                                                :context [:tierced-per-pairle :left]})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" bottom-adjusted
                            (line/stitch line-bottom-reversed)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get fields 2) environment-3 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-bottom)])}]])]))

(defn tierced-per-pairle-reversed [{:keys [fields line] :as field} environment top-level-render options]
  (let [mask-id-1                          (svg/id "division-tierced-pairle-reversed-1")
        mask-id-2                          (svg/id "division-tierced-pairle-reversed-2")
        mask-id-3                          (svg/id "division-tierced-pairle-reversed-3")
        line-style                         (or (:style line) :straight)
        top-left                           (get-in environment [:points :top-left])
        top-right                          (get-in environment [:points :top-right])
        top                                (get-in environment [:points :top])
        fess                               (get-in environment [:points :fess])
        bend-intersection-left             (v/project top-right fess (:x top-left))
        bend-intersection-right            (v/project top-left fess (:x top-right))
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line-style
                                                        (v/abs (v/- bend-intersection-right fess))
                                                        :angle -135
                                                        :reversed? true)
        {line-bottom-left :line}           (line/create line-style
                                                        (v/abs (v/- bend-intersection-left fess))
                                                        :angle -225
                                                        :flipped? true)
        {line-top :line}                   (line/create line-style
                                                        (v/abs (v/- top fess))
                                                        :flipped? true
                                                        :angle -90)
        {line-top-reversed        :line
         line-top-reversed-length :length} (line/create line-style
                                                        (v/abs (v/- top fess))
                                                        :angle 90
                                                        :reversed? true)
        bend-intersection-right-adjusted   (v/extend
                                               fess
                                             bend-intersection-right
                                             line-bottom-right-length)
        top-adjusted                       (v/extend
                                               fess
                                             top
                                             line-top-reversed-length)
        environment-1                      (field-environment/create
                                            (svg/make-path ["M" top-adjusted
                                                            (line/stitch line-top-reversed)
                                                            "L" fess
                                                            (line/stitch line-bottom-left)
                                                            (infinity/path :clockwise
                                                                           [:bottom-left :top]
                                                                           [bend-intersection-left top-adjusted])
                                                            "z"])
                                            {:parent  field
                                             :context [:tierced-per-pairle-reversed :left]})
        environment-2                      (field-environment/create
                                            (svg/make-path ["M" bend-intersection-right-adjusted
                                                            (line/stitch line-bottom-right)
                                                            "L" fess
                                                            (line/stitch line-top)
                                                            (infinity/path :clockwise
                                                                           [:top :bottom-right]
                                                                           [top bend-intersection-right-adjusted])
                                                            "z"])
                                            {:parent  field
                                             :context [:tierced-per-pairle-reversed :right]})
        environment-3                      (field-environment/create
                                            (svg/make-path ["M" bend-intersection-right-adjusted
                                                            (line/stitch line-bottom-right)
                                                            "L" fess
                                                            (line/stitch line-bottom-left)
                                                            (infinity/path :counter-clockwise
                                                                           [:bottom-left :bottom-right]
                                                                           [bend-intersection-left bend-intersection-right-adjusted])
                                                            "z"])
                                            {:parent  field
                                             :context [:tierced-per-pairle-reversed :bottom]})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d    (:shape environment-1)
               :fill "#fff"}]
       [:path.overlap {:d (:shape environment-1)}]]
      [:mask {:id mask-id-2}
       [:path {:d    (:shape environment-2)
               :fill "#fff"}]
       [:path.overlap {:d (svg/make-path
                           ["M" bend-intersection-right-adjusted
                            (line/stitch line-bottom-right)])}]]
      [:mask {:id mask-id-3}
       [:path {:d    (:shape environment-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (get fields 0) environment-1 options]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (get fields 1) environment-2 options]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (get fields 2) environment-3 options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" bend-intersection-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" fess
                     (line/stitch line-bottom-left)])}]])]))

(defn part-name [type index]
  (-> {:per-saltire ["I." "III." "IV." "II."]
       :quarterly   ["I." "II." "IV." "III."]
       :gyronny     ["I." "II." "IV." "VI." "VIII." "VII." "V." "III."]}
      (get-in [type index])))

;; TODO: this doesn't work well with per-saltire, because the 2nd field should be on the left,
;; but the first two should always be at index 0, 1; perhaps for per-saltire it needs to be
;; rendered counter clockwise?
(defn part-position [type index]
  (-> {:per-saltire [1 2 4 3]
       :quarterly   [1 2 4 3]
       :gyronny     [1 2 4 6 8 7 5 3]}
      (get-in [type index])
      (or index)))

(def kinds
  [["Per Pale" :per-pale per-pale]
   ["Per Fess" :per-fess per-fess]
   ["Per Bend" :per-bend per-bend]
   ["Per Bend Sinister" :per-bend-sinister per-bend-sinister]
   ["Per Chevron" :per-chevron per-chevron]
   ["Per Saltire" :per-saltire per-saltire]
   ["Quarterly" :quarterly quarterly]
   ["Gyronny" :gyronny gyronny]
   ["Tierced per Pale" :tierced-per-pale tierced-per-pale]
   ["Tierced per Fess" :tierced-per-fess tierced-per-fess]
   ["Tierced per Pairle" :tierced-per-pairle tierced-per-pairle]
   ["Tierced per Pairle Reversed" :tierced-per-pairle-reversed tierced-per-pairle-reversed]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn render [{:keys [type] :as division} environment top-level-render options]
  (let [function (get kinds-function-map type)]
    [function division environment top-level-render options]))

(defn mandatory-part-count [type]
  (case type
    nil                          0
    :tierced-per-pale            3
    :tierced-per-fess            3
    :tierced-per-pairle          3
    :tierced-per-pairle-reversed 3
    2))
