(ns or.coad.division
  (:require [or.coad.field-environment :as field-environment]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn get-field [fields index]
  (let [part (get fields index)
        ref (:ref part)]
    (if ref
      (assoc (get fields ref) :ui (:ui part))
      part)))

(defn make-division [type fields parts mask-overlaps outline parent-environment parent top-level-render options & {:keys [db-path]}]
  (let [mask-ids (->> (range (count fields))
                      (map #(svg/id (str (name type) "-" %))))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box]]
                                         (cond->
                                          (field-environment/create
                                           (svg/make-path shape-path)
                                           {:parent parent
                                            :context [type idx]
                                            :bounding-box (svg/bounding-box bounding-box)})
                                           (:inherit-environment? (get-field fields idx)) (assoc :points (:points parent-environment)))))
                          vec)]
    [:<>
     [:defs
      (for [[idx mask-id] (map-indexed vector mask-ids)]
        (let [environment-shape (:shape (get environments idx))
              overlap-paths (get mask-overlaps idx)]
          ^{:key idx}
          [:clipPath {:id mask-id}
           [:path {:d environment-shape
                   :fill "#fff"}]
           (cond
             (= overlap-paths :all) [:path.overlap {:d environment-shape}]
             overlap-paths (for [[idx shape] (map-indexed vector overlap-paths)]
                             ^{:key idx}
                             [:path.overlap {:d shape}]))]))]
     (for [[idx mask-id] (map-indexed vector mask-ids)]
       ^{:key idx}
       [:g {:clip-path (str "url(#" mask-id ")")}
        [top-level-render
         (get-field fields idx)
         (get environments idx)
         options
         :db-path (conj db-path :fields idx)]])
     outline]))

(defn per-pale [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [top-left (get-in environment [:points :top-left])
        bottom-right (get-in environment [:points :bottom-right])
        top (get-in environment [:points :top])
        bottom (get-in environment [:points :bottom])
        line-style (or (:style line) :straight)
        {line :line} (line/create line-style
                                  (:y (v/- bottom top))
                                  :angle -90
                                  :options options)
        parts [[["M" bottom
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:top :bottom]
                                [top bottom])
                 "z"]
                [top-left bottom]]

               [["M" bottom
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:top :bottom]
                                [top bottom])
                 "z"]
                [top bottom-right]]]]
    [make-division
     :division-per-pale fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bottom
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-fess [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [top-left (get-in environment [:points :top-left])
        bottom-right (get-in environment [:points :bottom-right])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        line-style (or (:style line) :straight)
        {line :line} (line/create line-style
                                  (:x (v/- right left))
                                  :options options)
        parts [[["M" left
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [right left])
                 "z"]
                [top-left right]]

               [["M" left
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:right :left]
                                [right left])
                 "z"]
                [left bottom-right]]]]
    [make-division
     :division-per-fess fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" left
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-bend [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        fess (get-in environment [:points :fess])
        bend-intersection (v/project top-left fess (:x top-right))
        line-style (or (:style line) :straight)
        {line :line} (line/create line-style
                                  (v/abs (v/- bend-intersection top-left))
                                  :angle 45
                                  :options options)
        parts [[["M" top-left
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:right :top-left]
                                [bend-intersection top-left])
                 "z"]
                [top-left bend-intersection]]
               [["M" top-left
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:right :top-left]
                                [bend-intersection top-left])
                 "z"]
                [top-left bend-intersection]]]]
    [make-division
     :division-per-bend fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-left
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-bend-sinister [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom-left (get-in environment [:points :bottom-left])
        fess (get-in environment [:points :fess])
        bend-intersection (v/project top-right fess (:x top-left))
        line-style (or (:style line) :straight)
        {line :line
         line-length :length} (line/create line-style
                                           (v/abs (v/- bend-intersection top-right))
                                           :angle -45
                                           :flipped? false
                                           :options options)
        bend-intersection-adjusted (v/extend
                                    top-right
                                     bend-intersection
                                     line-length)
        parts [[["M" bend-intersection-adjusted
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:top-right :left]
                                [top-right bend-intersection])
                 "z"]
                [top-right bend-intersection]]

               [["M" bend-intersection-adjusted
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:top-right :left]
                                [top-right bend-intersection])
                 "z"]
                [top-right bottom-left]]]]
    [make-division
     :division-per-bend-sinister fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bend-intersection-adjusted
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-chevron [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        fess (get-in environment [:points :fess])
        bend-intersection-left (v/project top-right fess (:x top-left))
        bend-intersection-right (v/project top-left fess (:x top-right))
        {line-left :line
         line-left-length :length} (line/create line-style
                                                (v/abs (v/- bend-intersection-left fess))
                                                :angle -45
                                                :options options)
        {line-right :line} (line/create line-style
                                        (v/abs (v/- bend-intersection-right fess))
                                        :angle 45
                                        :options options)
        bend-intersection-left-adjusted (v/extend fess bend-intersection-left line-left-length)
        parts [[["M" bend-intersection-left-adjusted
                 (line/stitch line-left)
                 "L" fess
                 (line/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [bend-intersection-right bend-intersection-left])
                 "z"]
                [top-left bottom-right]]

               [["M" bend-intersection-left-adjusted
                 (line/stitch line-left)
                 "L" fess
                 (line/stitch line-right)
                 (infinity/path :clockwise
                                [:right :left]
                                [bend-intersection-right bend-intersection-left])
                 "z"
                 "z"]
                [bottom-left fess bottom-right]]]]
    [make-division
     :division-per-chevron fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bend-intersection-left-adjusted
                     (line/stitch line-left)
                     "L" fess
                     (line/stitch line-right)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-saltire [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        fess (get-in environment [:points :fess])
        bend-intersection-right (v/project top-left fess (:x top-right))
        bend-intersection-left (v/project top-right fess (:x top-left))
        {line-top-left :line
         line-top-left-length :length} (line/create line-style
                                                    (v/abs (v/- top-left fess))
                                                    :angle 45
                                                    :reversed? true
                                                    :options options)
        {line-top-right :line} (line/create line-style
                                            (v/abs (v/- top-right fess))
                                            :angle -45
                                            :flipped? true
                                            :options options)
        {line-bottom-right :line
         line-bottom-right-length :length} (line/create line-style
                                                        (v/abs (v/- bend-intersection-right fess))
                                                        :angle 225
                                                        :reversed? true
                                                        :options options)
        {line-bottom-left :line} (line/create line-style
                                              (v/abs (v/- bend-intersection-left fess))
                                              :angle -225
                                              :flipped? true
                                              :options options)
        top-left-adjusted (v/extend
                           fess
                            top-left
                            line-top-left-length)
        bend-intersection-right-adjusted (v/extend
                                          fess
                                           bend-intersection-right
                                           line-bottom-right-length)
        parts [[["M" top-left-adjusted
                 (line/stitch line-top-left)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:top-right :top-left]
                                [top-right top-left])
                 "z"]
                [top-left fess top-right]]

               [["M" bend-intersection-right-adjusted
                 (line/stitch line-bottom-right)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :clockwise
                                [:top-right :bottom]
                                [top-right bend-intersection-right])
                 "z"]
                [top-right fess bottom-right]]

               [["M" bend-intersection-right-adjusted
                 (line/stitch line-bottom-right)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [bend-intersection-left bend-intersection-right])
                 "z"]
                [bottom-left fess bottom-right]]

               [["M" top-left-adjusted
                 (line/stitch line-top-left)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:bottom-left :top-left]
                                [bend-intersection-left top-left])
                 "z"]
                [top-left fess bottom-left]]]]
    [make-division
     :division-per-saltire fields parts
     [:all
      [(svg/make-path
        ["M" bend-intersection-right-adjusted
         (line/stitch line-bottom-right)])]
      [(svg/make-path
        ["M" fess
         (line/stitch line-bottom-left)])]
      nil]
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
                     (line/stitch line-bottom-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn quarterly [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        top (get-in environment [:points :top])
        bottom (get-in environment [:points :bottom])
        fess (get-in environment [:points :fess])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        {line-top :line
         line-top-length :length} (line/create line-style
                                               (v/abs (v/- top fess))
                                               :angle 90
                                               :reversed? true
                                               :options options)
        {line-right :line} (line/create line-style
                                        (v/abs (v/- right fess))
                                        :flipped? true
                                        :options options)
        {line-bottom :line
         line-bottom-length :length} (line/create line-style
                                                  (v/abs (v/- bottom fess))
                                                  :angle -90
                                                  :reversed? true
                                                  :options options)
        {line-left :line} (line/create line-style
                                       (v/abs (v/- left fess))
                                       :angle -180
                                       :flipped? true
                                       :options options)
        top-adjusted (v/extend fess top line-top-length)
        bottom-adjusted (v/extend fess bottom line-bottom-length)
        parts [[["M" top-adjusted
                 (line/stitch line-top)
                 "L" fess
                 (line/stitch line-left)
                 (infinity/path :clockwise
                                [:left :top]
                                [left top])
                 "z"]
                [top-left fess]]

               [["M" top-adjusted
                 (line/stitch line-top)
                 "L" fess
                 (line/stitch line-right)
                 (infinity/path :counter-clockwise
                                [:right :top]
                                [right top])
                 "z"]
                [fess top-right]]

               [["M" bottom-adjusted
                 (line/stitch line-bottom)
                 "L" fess
                 (line/stitch line-right)
                 (infinity/path :clockwise
                                [:right :bottom]
                                [right bottom])
                 "z"]
                [fess bottom-right]]

               [["M" bottom-adjusted
                 (line/stitch line-bottom)
                 "L" fess
                 (line/stitch line-left)
                 (infinity/path :counter-clockwise
                                [:left :bottom]
                                [left bottom])
                 "z"]
                [fess bottom-left]]]]
    [make-division
     :division-quarterly fields parts
     [:all
      [(svg/make-path
        ["M" fess
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom)])]
      nil]
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
                     (line/stitch line-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn gyronny [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        top (get-in environment [:points :top])
        bottom (get-in environment [:points :bottom])
        fess (get-in environment [:points :fess])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        {line-top :line
         line-top-length :length} (line/create line-style
                                               (v/abs (v/- top fess))
                                               :angle 90
                                               :reversed? true
                                               :options options)
        {line-right :line
         line-right-length :length} (line/create line-style
                                                 (v/abs (v/- right fess))
                                                 :reversed? true
                                                 :angle 180
                                                 :options options)
        {line-bottom :line
         line-bottom-length :length} (line/create line-style
                                                  (v/abs (v/- bottom fess))
                                                  :angle -90
                                                  :reversed? true
                                                  :options options)
        {line-left :line
         line-left-length :length} (line/create line-style
                                                (v/abs (v/- left fess))
                                                :reversed? true
                                                :options options)
        top-adjusted (v/extend fess top line-top-length)
        bottom-adjusted (v/extend fess bottom line-bottom-length)
        left-adjusted (v/extend fess left line-left-length)
        right-adjusted (v/extend fess right line-right-length)
        bend-intersection-left (v/project top-right fess (:x top-left))
        bend-intersection-right (v/project top-left fess (:x top-right))
        {line-top-left :line} (line/create line-style
                                           (v/abs (v/- top-left fess))
                                           :flipped? true
                                           :angle -135
                                           :options options)
        {line-top-right :line} (line/create line-style
                                            (v/abs (v/- top-right fess))
                                            :flipped? true
                                            :angle -45
                                            :options options)
        {line-bottom-right :line} (line/create line-style
                                               (v/abs (v/- bend-intersection-right fess))
                                               :flipped? true
                                               :angle 45
                                               :options options)
        {line-bottom-left :line} (line/create line-style
                                              (v/abs (v/- bend-intersection-left fess))
                                              :flipped? true
                                              :angle -225
                                              :options options)
        parts [[["M" top-adjusted
                 (line/stitch line-top)
                 "L" fess
                 (line/stitch line-top-left)
                 (infinity/path :clockwise
                                [:top-left :top]
                                [top-left top])
                 "z"]
                [top-left fess top]]

               [["M" top-adjusted
                 (line/stitch line-top)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:top-right :top]
                                [top-right top])
                 "z"]
                [top fess top-right]]

               [["M" right-adjusted
                 (line/stitch line-right)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :clockwise
                                [:top-right :right]
                                [top-right right])
                 "z"]
                [top-right fess right]]

               [["M" right-adjusted
                 (line/stitch line-right)
                 "L" fess
                 (line/stitch line-bottom-right)
                 (infinity/path :counter-clockwise
                                [:bottom-right :right]
                                [bend-intersection-right right])
                 "z"]
                [right fess bottom-right]]

               [["M" bottom-adjusted
                 (line/stitch line-bottom)
                 "L" fess
                 (line/stitch line-bottom-right)
                 (infinity/path :clockwise
                                [:bottom-right :bottom]
                                [bend-intersection-right bottom])
                 "z"]
                [bottom-right fess bottom]]

               [["M" bottom-adjusted
                 (line/stitch line-bottom)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:bottom-left :bottom]
                                [bend-intersection-left bottom])
                 "z"]
                [bottom fess bottom-left]]

               [["M" left-adjusted
                 (line/stitch line-left)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:bottom-left :left]
                                [bend-intersection-left left])
                 "z"]
                [bottom-left fess left]]

               [["M" left-adjusted
                 (line/stitch line-left)
                 "L" fess
                 (line/stitch line-top-left)
                 (infinity/path :counter-clockwise
                                [:top-left :left]
                                [top-left left])
                 "z"]
                [left fess top-left]]]]
    [make-division
     :division-gyronny fields parts
     [:all
      [(svg/make-path
        ["M" fess
         (line/stitch line-top-right)])]
      [(svg/make-path
        ["M" right-adjusted
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" fess
         (line/stitch line-bottom-right)])]
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom)])]
      [(svg/make-path
        ["M" fess
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" left-adjusted
         (line/stitch line-left)])]
      nil]
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
                     (line/stitch line-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-pale [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top (get-in environment [:points :top])
        top-left (get-in environment [:points :top-left])
        bottom (get-in environment [:points :bottom])
        bottom-right (get-in environment [:points :bottom-right])
        fess (get-in environment [:points :fess])
        width (:width environment)
        col1 (- (:x fess) (/ width 6))
        col2 (+ (:x fess) (/ width 6))
        first-top (v/v col1 (:y top))
        first-bottom (v/v col1 (:y bottom))
        second-top (v/v col2 (:y top))
        second-bottom (v/v col2 (:y bottom))
        {line :line} (line/create line-style
                                  (:y (v/- bottom top))
                                  :flipped? true
                                  :angle 90
                                  :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true
                                                    :options options)
        second-bottom-adjusted (v/extend second-top second-bottom line-reversed-length)
        parts [[["M" first-top
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:bottom :top]
                                [first-bottom first-top])
                 "z"]
                [top-left first-bottom]]

               [["M" second-bottom-adjusted
                 (line/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:top :top]
                                [second-top first-top])
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:bottom :bottom]
                                [first-bottom second-bottom])
                 "z"]
                [first-top second-bottom]]

               [["M" second-bottom-adjusted
                 (line/stitch line-reversed)
                 (infinity/path :clockwise
                                [:top :bottom]
                                [second-top second-bottom])
                 "z"]
                [second-top bottom-right]]]]
    [make-division
     :division-tierced-per-pale fields parts
     [:all
      [(svg/make-path
        ["M" second-bottom-adjusted
         (line/stitch line-reversed)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-fess [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        bottom-right (get-in environment [:points :bottom-right])
        fess (get-in environment [:points :fess])
        height (:height environment)
        row1 (- (:y fess) (/ height 6))
        row2 (+ (:y fess) (/ height 6))
        first-left (v/v (:x left) row1)
        first-right (v/v (:x right) row1)
        second-left (v/v (:x left) row2)
        second-right (v/v (:x right) row2)
        {line :line} (line/create line-style
                                  (:x (v/- right left))
                                  :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        second-right-adjusted (v/extend second-left second-right line-reversed-length)
        parts [[["M" first-left
                 (line/stitch line)
                 (infinity/path :counter-clockwise
                                [:right :left]
                                [first-right first-left])
                 "z"]
                [top-left first-right]]

               [["M" first-left
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:right :right]
                                [first-right second-right-adjusted])
                 (line/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :left]
                                [second-left first-left])
                 "z"]
                [first-left second-right]]

               [["M" second-right-adjusted
                 (line/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:left :right]
                                [second-left second-right-adjusted])
                 "z"]
                [second-left bottom-right]]]]
    [make-division
     :division-tierced-per-fess fields parts
     [:all
      [(svg/make-path
        ["M" second-right-adjusted
         (line/stitch line-reversed)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-pairle [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        bottom (get-in environment [:points :bottom])
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        fess (get-in environment [:points :fess])
        {line-top-left :line
         line-top-left-length :length} (line/create line-style
                                                    (v/abs (v/- top-left fess))
                                                    :angle 45
                                                    :reversed? true
                                                    :options options)
        {line-top-right :line} (line/create line-style
                                            (v/abs (v/- top-right fess))
                                            :angle -45
                                            :flipped? true
                                            :options options)
        {line-bottom :line} (line/create line-style
                                         (v/abs (v/- bottom fess))
                                         :flipped? true
                                         :angle 90
                                         :options options)
        {line-bottom-reversed :line
         line-bottom-reversed-length :length} (line/create line-style
                                                           (v/abs (v/- bottom fess))
                                                           :angle -90
                                                           :reversed? true
                                                           :options options)
        top-left-adjusted (v/extend
                           fess
                            top-left
                            line-top-left-length)
        bottom-adjusted (v/extend
                         fess
                          bottom
                          line-bottom-reversed-length)
        parts [[["M" top-left-adjusted
                 (line/stitch line-top-left)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :counter-clockwise
                                [:top-right :top-left]
                                [top-right top-left])
                 "z"]
                [top-left fess top-right]]

               [["M" bottom-adjusted
                 (line/stitch line-bottom-reversed)
                 "L" fess
                 (line/stitch line-top-right)
                 (infinity/path :clockwise
                                [:top-right :bottom]
                                [top-right bottom])
                 "z"]
                [fess top-right bottom-right bottom]]

               [["M" top-left-adjusted
                 (line/stitch line-top-left)
                 "L" fess
                 (line/stitch line-bottom)
                 (infinity/path :clockwise
                                [:bottom :top-left]
                                [bottom top-left])
                 "z"]
                [top-left fess bottom bottom-left]]]]
    [make-division
     :division-tierced-per-fess fields parts
     [:all
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom-reversed)])]
      nil]
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
                     (line/stitch line-bottom)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-pairle-reversed [{:keys [fields line] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        top (get-in environment [:points :top])
        fess (get-in environment [:points :fess])
        bend-intersection-left (v/project top-right fess (:x top-left))
        bend-intersection-right (v/project top-left fess (:x top-right))
        {line-bottom-right :line
         line-bottom-right-length :length} (line/create line-style
                                                        (v/abs (v/- bend-intersection-right fess))
                                                        :angle -135
                                                        :reversed? true
                                                        :options options)
        {line-bottom-left :line} (line/create line-style
                                              (v/abs (v/- bend-intersection-left fess))
                                              :angle -225
                                              :flipped? true
                                              :options options)
        {line-top :line} (line/create line-style
                                      (v/abs (v/- top fess))
                                      :flipped? true
                                      :angle -90
                                      :options options)
        {line-top-reversed :line
         line-top-reversed-length :length} (line/create line-style
                                                        (v/abs (v/- top fess))
                                                        :angle 90
                                                        :reversed? true
                                                        :options options)
        bend-intersection-right-adjusted (v/extend
                                          fess
                                           bend-intersection-right
                                           line-bottom-right-length)
        top-adjusted (v/extend
                      fess
                       top
                       line-top-reversed-length)
        parts [[["M" top-adjusted
                 (line/stitch line-top-reversed)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :clockwise
                                [:bottom-left :top]
                                [bend-intersection-left top-adjusted])
                 "z"]
                [top-left top fess bend-intersection-left]]

               [["M" bend-intersection-right-adjusted
                 (line/stitch line-bottom-right)
                 "L" fess
                 (line/stitch line-top)
                 (infinity/path :clockwise
                                [:top :bottom-right]
                                [top bend-intersection-right-adjusted])
                 "z"]
                [top top-right bend-intersection-right fess]]

               [["M" bend-intersection-right-adjusted
                 (line/stitch line-bottom-right)
                 "L" fess
                 (line/stitch line-bottom-left)
                 (infinity/path :counter-clockwise
                                [:bottom-left :bottom-right]
                                [bend-intersection-left bend-intersection-right-adjusted])
                 "z"]
                [fess bend-intersection-right bend-intersection-left]]]]
    [make-division
     :division-tierced-per-fess fields parts
     [:all
      [(svg/make-path
        ["M" bend-intersection-right-adjusted
         (line/stitch line-bottom-right)])]
      nil]
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
                     (line/stitch line-bottom-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn part-name [type index]
  (-> {:per-saltire ["I." "III." "IV." "II."]
       :quarterly ["I." "II." "IV." "III."]
       :gyronny ["I." "II." "IV." "VI." "VIII." "VII." "V." "III."]}
      (get-in [type index])))

;; TODO: this doesn't work well with per-saltire, because the 2nd field should be on the left,
;; but the first two should always be at index 0, 1; perhaps for per-saltire it needs to be
;; rendered counter clockwise?
(defn part-position [type index]
  (-> {:per-saltire [1 2 4 3]
       :quarterly [1 2 4 3]
       :gyronny [1 2 4 6 8 7 5 3]}
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
              [name key]))))

(defn render [{:keys [type] :as division} environment top-level-render options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    [function division environment top-level-render options :db-path db-path]))

(defn mandatory-part-count [type]
  (case type
    nil 0
    :tierced-per-pale 3
    :tierced-per-fess 3
    :tierced-per-pairle 3
    :tierced-per-pairle-reversed 3
    2))
