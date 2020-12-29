(ns or.coad.division
  (:require [clojure.string :as s]
            [or.coad.config :as config]
            [or.coad.field-environment :as field-environment]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.point :as point]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn default-fields [type]
  (into [config/default-field
         (-> config/default-field
             (assoc-in [:content :tincture] :azure))]
        (cond
          (= :per-saltire type)                  [{:ref 1} {:ref 0}]
          (= :quarterly type)                    [{:ref 1} {:ref 0}]
          (= :gyronny type)                      [{:ref 1} {:ref 0} {:ref 0} {:ref 1} {:ref 1} {:ref 0}]
          (#{:tierced-per-pale
             :tierced-per-fess
             :tierced-per-pairle
             :tierced-per-pairle-reversed} type) [(-> config/default-field
                                                      (assoc-in [:content :tincture] :gules))])))

(defn diagonal-default [type]
  (or (get {:per-bend-sinister           :top-right-fess
            :per-chevron                 :forty-five-degrees
            :tierced-per-pairle-reversed :forty-five-degrees} type)
      :top-left-fess))

(defn get-field [fields index]
  (let [part (get fields index)
        ref  (:ref part)]
    (if ref
      (assoc (get fields ref) :ui (:ui part))
      part)))

(defn division-context-key [key]
  (keyword (str "division-" (name key))))

(defn make-division [type fields parts mask-overlaps outline parent-environment parent
                     top-level-render options & {:keys [db-path]}]
  (let [mask-ids     (->> (range (count fields))
                          (map #(svg/id (str (name type) "-" %))))
        environments (->> parts
                          (map-indexed (fn [idx [shape-path bounding-box]]
                                         (let [field (get-field fields idx)]
                                           (field-environment/create
                                            (svg/make-path shape-path)
                                            {:parent               parent
                                             :context              [type idx]
                                             :bounding-box         (svg/bounding-box bounding-box)
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (:counterchanged? field))
                                                                     parent-environment)}))))
                          vec)]
    [:<>
     [:defs
      (for [[idx mask-id] (map-indexed vector mask-ids)]
        (let [environment-shape (:shape (get environments idx))
              overlap-paths     (get mask-overlaps idx)]
          ^{:key idx}
          [:clipPath {:id mask-id}
           [:path {:d    environment-shape
                   :fill "#fff"}]
           (cond
             (= overlap-paths :all) [:path.overlap {:d environment-shape}]
             overlap-paths          (for [[idx shape] (map-indexed vector overlap-paths)]
                                      ^{:key idx}
                                      [:path.overlap {:d shape}]))]))]
     (for [[idx mask-id] (map-indexed vector mask-ids)]
       ^{:key idx}
       [:g {:clip-path (str "url(#" mask-id ")")}
        [top-level-render
         (get-field fields idx)
         (get environments idx)
         options
         :db-path (if (-> type name (s/starts-with? "ordinary-")) ;; FIXME: bit of a hack
                    (conj db-path :field)
                    (conj db-path :fields idx))]])
     outline]))

(defn per-pale [{:keys [type fields line origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                     (:points environment)
        origin-point               (point/calculate origin environment :fess)
        top-left                   (:top-left points)
        top                        (assoc (:top points) :x (:x origin-point))
        bottom                     (assoc (:bottom points) :x (:x origin-point))
        bottom-right               (:bottom-right points)
        {line        :line
         line-length :line-length} (line/create line
                                                (:y (v/- bottom top))
                                                :angle 90
                                                :reversed? true
                                                :options options)
        bottom-adjusted            (v/extend top bottom line-length)
        parts                      [[["M" bottom-adjusted
                                      (line/stitch line)
                                      (infinity/path :clockwise
                                                     [:bottom :top]
                                                     [bottom-adjusted top])
                                      "z"]
                                     [top-left bottom]]

                                    [["M" bottom-adjusted
                                      (line/stitch line)
                                      (infinity/path :counter-clockwise
                                                     [:bottom :top]
                                                     [bottom-adjusted top])
                                      "z"]
                                     [top bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-fess [{:keys [type fields line origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points       (:points environment)
        origin-point (point/calculate origin environment :fess)
        top-left     (:top-left points)
        left         (assoc (:left points) :y (:y origin-point))
        right        (assoc (:right points) :y (:y origin-point))
        bottom-right (:bottom-right points)
        {line :line} (line/create line
                                  (:x (v/- right left))
                                  :options options)
        parts        [[["M" left
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
     (division-context-key type) fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" left
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn angle-to-point [p1 p2]
  (let [d         (v/- p2 p1)
        angle-rad (Math/atan2 (:y d) (:x d))]
    (-> angle-rad
        (/ Math/PI)
        (* 180))))

(defn direction [diagonal-mode points & [origin]]
  (let [top-left      (:top-left points)
        top-right     (:top-right points)
        bottom-left   (:bottom-left points)
        bottom-right  (:bottom-right points)
        origin        (or origin (:fess points))
        origin-height (-> origin
                          (v/- top-left)
                          :y)
        dir           (case diagonal-mode
                        :top-left-fess     (v/- origin top-left)
                        :top-right-fess    (v/- origin top-right)
                        :bottom-left-fess  (v/- origin bottom-left)
                        :bottom-right-fess (v/- origin bottom-right)
                        (v/v origin-height origin-height))]
    (v/v (-> dir :x Math/abs)
         (-> dir :y Math/abs))))

(defn per-bend [{:keys [type fields line hints origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points         (:points environment)
        origin-point   (point/calculate origin environment :fess)
        top-left       (:top-left points)
        top            (:top points)
        bottom         (:bottom points)
        left           (:left points)
        right          (:right points)
        diagonal-mode  (or (:diagonal-mode hints)
                           (diagonal-default type))
        direction      (direction diagonal-mode points origin-point)
        diagonal-start (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end   (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle          (angle-to-point diagonal-start diagonal-end)
        {line :line}   (line/create line
                                    (v/abs (v/- diagonal-end diagonal-start))
                                    :angle angle
                                    :options options)
        parts          [[["M" diagonal-start
                          (line/stitch line)
                          (infinity/path :counter-clockwise
                                         [:right :top]
                                         [diagonal-end diagonal-start])
                          "z"]
                         [diagonal-start top diagonal-end]]
                        [["M" diagonal-start
                          (line/stitch line)
                          (infinity/path :clockwise
                                         [:right :top]
                                         [diagonal-end diagonal-start])
                          "z"]
                         [diagonal-start diagonal-end bottom]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" top-left
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-bend-sinister [{:keys [type fields line hints origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                (:points environment)
        origin-point          (point/calculate origin environment :fess)
        top                   (:top points)
        bottom                (:bottom points)
        left                  (:left points)
        right                 (:right points)
        diagonal-mode         (or (:diagonal-mode hints)
                                  (diagonal-default type))
        direction             (direction diagonal-mode points origin-point)
        diagonal-start        (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-end          (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        angle                 (angle-to-point diagonal-start diagonal-end)
        {line        :line
         line-length :length} (line/create line
                                           (v/abs (v/- diagonal-end diagonal-start))
                                           :angle (+ angle 180)
                                           :reversed? true
                                           :options options)
        diagonal-end-adjusted (v/extend
                                  diagonal-start
                                diagonal-end
                                line-length)
        parts                 [[["M" diagonal-end-adjusted
                                 (line/stitch line)
                                 (infinity/path :counter-clockwise
                                                [:top :left]
                                                [diagonal-start diagonal-end])
                                 "z"]
                                [diagonal-start top diagonal-end]]

                               [["M" diagonal-end-adjusted
                                 (line/stitch line)
                                 (infinity/path :clockwise
                                                [:top :left]
                                                [diagonal-start diagonal-end])
                                 "z"]
                                [diagonal-start bottom diagonal-end]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" diagonal-end-adjusted
                     (line/stitch line)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-chevron [{:keys [type fields line origin hints] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                        (:points environment)
        origin-point                  (point/calculate origin environment :fess)
        top-left                      (:top-left points)
        bottom-left                   (:bottom-left points)
        bottom-right                  (:bottom-right points)
        left                          (:left points)
        right                         (:right points)
        diagonal-mode                 (or (:diagonal-mode hints)
                                          (diagonal-default type))
        direction                     (direction diagonal-mode points origin-point)
        diagonal-bottom-left          (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right         (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left             (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right            (angle-to-point origin-point diagonal-bottom-right)
        line                          (-> line
                                          (update :offset max 0))
        {line-left        :line
         line-left-length :length}    (line/create line
                                                   (v/abs (v/- diagonal-bottom-left origin-point))
                                                   :angle (+ angle-bottom-left 180)
                                                   :reversed? true
                                                   :options options)
        {line-right :line}            (line/create line
                                                   (v/abs (v/- diagonal-bottom-right origin-point))
                                                   :angle angle-bottom-right
                                                   :options options)
        diagonal-bottom-left-adjusted (v/extend origin-point diagonal-bottom-left line-left-length)
        parts                         [[["M" diagonal-bottom-left-adjusted
                                         (line/stitch line-left)
                                         "L" origin-point
                                         (line/stitch line-right)
                                         (infinity/path :counter-clockwise
                                                        [:right :left]
                                                        [diagonal-bottom-right diagonal-bottom-left])
                                         "z"]
                                        [top-left bottom-right]]

                                       [["M" diagonal-bottom-left-adjusted
                                         (line/stitch line-left)
                                         "L" origin-point
                                         (line/stitch line-right)
                                         (infinity/path :clockwise
                                                        [:right :left]
                                                        [diagonal-bottom-right diagonal-bottom-left])
                                         "z"
                                         "z"]
                                        [bottom-left origin-point bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-left-adjusted
                     (line/stitch line-left)
                     "L" origin-point
                     (line/stitch line-right)])}]])
     environment field top-level-render options :db-path db-path]))

(defn per-saltire [{:keys [type fields line origin hints] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                             (:points environment)
        origin-point                       (point/calculate origin environment :fess)
        top-left                           (:top-left points)
        top-right                          (:top-right points)
        bottom-left                        (:bottom-left points)
        bottom-right                       (:bottom-right points)
        left                               (:left points)
        right                              (:right points)
        diagonal-mode                      (or (:diagonal-mode hints)
                                               (diagonal-default type))
        direction                          (direction diagonal-mode points origin-point)
        diagonal-top-left                  (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                 (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left               (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left                     (angle-to-point origin-point diagonal-top-left)
        angle-top-right                    (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                  (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                 (angle-to-point origin-point diagonal-bottom-right)
        line                               (-> line
                                               (update :offset max 0))
        {line-top-left        :line
         line-top-left-length :length}     (line/create line
                                                        (v/abs (v/- diagonal-top-left origin-point))
                                                        :angle (+ angle-top-left 180)
                                                        :reversed? true
                                                        :options options)
        {line-top-right :line}             (line/create line
                                                        (v/abs (v/- diagonal-top-right origin-point))
                                                        :angle angle-top-right
                                                        :flipped? true
                                                        :options options)
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line
                                                        (v/abs (v/- diagonal-bottom-right origin-point))
                                                        :angle (+ angle-bottom-right 180)
                                                        :reversed? true
                                                        :options options)
        {line-bottom-left :line}           (line/create line
                                                        (v/abs (v/- diagonal-bottom-left origin-point))
                                                        :angle angle-bottom-left
                                                        :flipped? true
                                                        :options options)
        diagonal-top-left-adjusted         (v/extend
                                               origin-point
                                             diagonal-top-left
                                             line-top-left-length)
        diagonal-bottom-right-adjusted     (v/extend
                                               origin-point
                                             diagonal-bottom-right
                                             line-bottom-right-length)
        parts                              [[["M" diagonal-top-left-adjusted
                                              (line/stitch line-top-left)
                                              "L" origin-point
                                              (line/stitch line-top-right)
                                              (infinity/path :counter-clockwise
                                                             [:right :left]
                                                             [diagonal-top-right diagonal-top-left])
                                              "z"]
                                             [top-left origin-point top-right]]

                                            [["M" diagonal-top-left-adjusted
                                              (line/stitch line-top-left)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :clockwise
                                                             [:left :left]
                                                             [diagonal-bottom-left diagonal-top-left-adjusted])
                                              "z"]
                                             [diagonal-top-left origin-point diagonal-bottom-left]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-top-right)
                                              (infinity/path :clockwise
                                                             [:right :right]
                                                             [diagonal-top-right diagonal-bottom-right])
                                              "z"]
                                             [diagonal-top-right origin-point diagonal-bottom-right]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :counter-clockwise
                                                             [:left :right]
                                                             [diagonal-bottom-left diagonal-bottom-right-adjusted])
                                              "z"]
                                             [bottom-left origin-point bottom-right]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" diagonal-bottom-right-adjusted
         (line/stitch line-bottom-right)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" diagonal-top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn quarterly [{:keys [type fields line origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                       (:points environment)
        origin-point                 (point/calculate origin environment :fess)
        top                          (assoc (:top points) :x (:x origin-point))
        top-left                     (:top-left points)
        top-right                    (:top-right points)
        bottom                       (assoc (:bottom points) :x (:x origin-point))
        bottom-left                  (:bottom-left points)
        bottom-right                 (:bottom-right points)
        left                         (assoc (:left points) :y (:y origin-point))
        right                        (assoc (:right points) :y (:y origin-point))
        line                         (-> line
                                         (update :offset max 0))
        {line-top        :line
         line-top-length :length}    (line/create line
                                                  (v/abs (v/- top origin-point))
                                                  :angle 90
                                                  :reversed? true
                                                  :options options)
        {line-right :line}           (line/create line
                                                  (v/abs (v/- right origin-point))
                                                  :flipped? true
                                                  :options options)
        {line-bottom        :line
         line-bottom-length :length} (line/create line
                                                  (v/abs (v/- bottom origin-point))
                                                  :angle -90
                                                  :reversed? true
                                                  :options options)
        {line-left :line}            (line/create line
                                                  (v/abs (v/- left origin-point))
                                                  :angle -180
                                                  :flipped? true
                                                  :options options)
        top-adjusted                 (v/extend origin-point top line-top-length)
        bottom-adjusted              (v/extend origin-point bottom line-bottom-length)
        parts                        [[["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-left)
                                        (infinity/path :clockwise
                                                       [:left :top]
                                                       [left top])
                                        "z"]
                                       [top-left origin-point]]

                                      [["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-right)
                                        (infinity/path :counter-clockwise
                                                       [:right :top]
                                                       [right top])
                                        "z"]
                                       [origin-point top-right]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-left)
                                        (infinity/path :counter-clockwise
                                                       [:left :bottom]
                                                       [left bottom])
                                        "z"]
                                       [origin-point bottom-left]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-right)
                                        (infinity/path :clockwise
                                                       [:right :bottom]
                                                       [right bottom])
                                        "z"]
                                       [origin-point bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
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
                    ["M" origin-point
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn gyronny [{:keys [type fields line origin hints] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                       (:points environment)
        origin-point                 (point/calculate origin environment :fess)
        top                          (assoc (:top points) :x (:x origin-point))
        bottom                       (assoc (:bottom points) :x (:x origin-point))
        left                         (assoc (:left points) :y (:y origin-point))
        right                        (assoc (:right points) :y (:y origin-point))
        diagonal-mode                (or (:diagonal-mode hints)
                                         (diagonal-default type))
        direction                    (direction diagonal-mode points origin-point)
        diagonal-top-left            (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right           (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left         (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right        (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left               (angle-to-point origin-point diagonal-top-left)
        angle-top-right              (angle-to-point origin-point diagonal-top-right)
        angle-bottom-left            (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right           (angle-to-point origin-point diagonal-bottom-right)
        line                         (-> line
                                         (update :offset max 0))
        {line-top        :line
         line-top-length :length}    (line/create line
                                                  (v/abs (v/- top origin-point))
                                                  :angle 90
                                                  :reversed? true
                                                  :options options)
        {line-right        :line
         line-right-length :length}  (line/create line
                                                  (v/abs (v/- right origin-point))
                                                  :reversed? true
                                                  :angle 180
                                                  :options options)
        {line-bottom        :line
         line-bottom-length :length} (line/create line
                                                  (v/abs (v/- bottom origin-point))
                                                  :angle -90
                                                  :reversed? true
                                                  :options options)
        {line-left        :line
         line-left-length :length}   (line/create line
                                                  (v/abs (v/- left origin-point))
                                                  :reversed? true
                                                  :options options)
        top-adjusted                 (v/extend origin-point top line-top-length)
        bottom-adjusted              (v/extend origin-point bottom line-bottom-length)
        left-adjusted                (v/extend origin-point left line-left-length)
        right-adjusted               (v/extend origin-point right line-right-length)
        {line-top-left :line}        (line/create line
                                                  (v/abs (v/- diagonal-top-left origin-point))
                                                  :flipped? true
                                                  :angle angle-top-left
                                                  :options options)
        {line-top-right :line}       (line/create line
                                                  (v/abs (v/- diagonal-top-right origin-point))
                                                  :flipped? true
                                                  :angle angle-top-right
                                                  :options options)
        {line-bottom-right :line}    (line/create line
                                                  (v/abs (v/- diagonal-bottom-right origin-point))
                                                  :flipped? true
                                                  :angle angle-bottom-right
                                                  :options options)
        {line-bottom-left :line}     (line/create line
                                                  (v/abs (v/- diagonal-bottom-left origin-point))
                                                  :flipped? true
                                                  :angle angle-bottom-left
                                                  :options options)
        parts                        [[["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-top-left)
                                        (infinity/path :clockwise
                                                       [:left :top]
                                                       [diagonal-top-left top])
                                        "z"]
                                       [diagonal-top-left origin-point top]]

                                      [["M" top-adjusted
                                        (line/stitch line-top)
                                        "L" origin-point
                                        (line/stitch line-top-right)
                                        (infinity/path :counter-clockwise
                                                       [:right :top]
                                                       [diagonal-top-right top])
                                        "z"]
                                       [top origin-point diagonal-top-right]]

                                      [["M" left-adjusted
                                        (line/stitch line-left)
                                        "L" origin-point
                                        (line/stitch line-top-left)
                                        (infinity/path :counter-clockwise
                                                       [:left :left]
                                                       [diagonal-top-left left])
                                        "z"]
                                       [left origin-point diagonal-top-left]]

                                      [["M" right-adjusted
                                        (line/stitch line-right)
                                        "L" origin-point
                                        (line/stitch line-top-right)
                                        (infinity/path :clockwise
                                                       [:right :right]
                                                       [diagonal-top-right right])
                                        "z"]
                                       [diagonal-top-right origin-point right]]

                                      [["M" left-adjusted
                                        (line/stitch line-left)
                                        "L" origin-point
                                        (line/stitch line-bottom-left)
                                        (infinity/path :clockwise
                                                       [:left :left]
                                                       [diagonal-bottom-left left])
                                        "z"]
                                       [diagonal-bottom-left origin-point left]]

                                      [["M" right-adjusted
                                        (line/stitch line-right)
                                        "L" origin-point
                                        (line/stitch line-bottom-right)
                                        (infinity/path :counter-clockwise
                                                       [:right :right]
                                                       [diagonal-bottom-right right])
                                        "z"]
                                       [right origin-point diagonal-bottom-right]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-bottom-left)
                                        (infinity/path :counter-clockwise
                                                       [:left :bottom]
                                                       [diagonal-bottom-left bottom])
                                        "z"]
                                       [bottom origin-point diagonal-bottom-left]]

                                      [["M" bottom-adjusted
                                        (line/stitch line-bottom)
                                        "L" origin-point
                                        (line/stitch line-bottom-right)
                                        (infinity/path :clockwise
                                                       [:right :bottom]
                                                       [diagonal-bottom-right bottom])
                                        "z"]
                                       [diagonal-bottom-right origin-point bottom]]]]

    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-top-right)])]
      [(svg/make-path
        ["M" left-adjusted
         (line/stitch line-left)])]
      [(svg/make-path
        ["M" right-adjusted
         (line/stitch line-right)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-left)])]
      [(svg/make-path
        ["M" origin-point
         (line/stitch line-bottom-right)])]

      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" top-adjusted
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" right-adjusted
                     (line/stitch line-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-adjusted
                     (line/stitch line-bottom)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" left-adjusted
                     (line/stitch line-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-pale [{:keys [type fields line origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (point/calculate origin environment :fess)
        top                            (assoc (:top points) :x (:x origin-point))
        top-left                       (:top-left points)
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        bottom-right                   (:bottom-right points)
        width                          (:width environment)
        col1                           (- (:x origin-point) (/ width 6))
        col2                           (+ (:x origin-point) (/ width 6))
        first-top                      (v/v col1 (:y top))
        first-bottom                   (v/v col1 (:y bottom))
        second-top                     (v/v col2 (:y top))
        second-bottom                  (v/v col2 (:y bottom))
        {line-one :line}               (line/create line
                                                    (:y (v/- bottom top))
                                                    :flipped? true
                                                    :angle 90
                                                    :options options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true
                                                    :options options)
        second-bottom-adjusted         (v/extend second-top second-bottom line-reversed-length)
        parts                          [[["M" first-top
                                          (line/stitch line-one)
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
                                          (line/stitch line-one)
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
     (division-context-key type) fields parts
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

(defn tierced-per-fess [{:keys [type fields line origin] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (point/calculate origin environment :fess)
        top-left                       (:top-left points)
        bottom-right                   (:bottom-right points)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        height                         (:height environment)
        row1                           (- (:y origin-point) (/ height 6))
        row2                           (+ (:y origin-point) (/ height 6))
        first-left                     (v/v (:x left) row1)
        first-right                    (v/v (:x right) row1)
        second-left                    (v/v (:x left) row2)
        second-right                   (v/v (:x right) row2)
        {line-one :line}               (line/create line
                                                    (:x (v/- right left))
                                                    :options options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        parts                          [[["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:right :left]
                                                         [first-right first-left])
                                          "z"]
                                         [top-left first-right]]

                                        [["M" first-left
                                          (line/stitch line-one)
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
     (division-context-key type) fields parts
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

(defn tierced-per-pairle [{:keys [type fields line origin hints] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                                (:points environment)
        origin-point                          (point/calculate origin environment :fess)
        bottom                                (assoc (:bottom points) :x (:x origin-point))
        bottom-left                           (:bottom-left points)
        bottom-right                          (:bottom-right points)
        left                                  (assoc (:left points) :y (:y origin-point))
        right                                 (assoc (:right points) :y (:y origin-point))
        diagonal-mode                         (or (:diagonal-mode hints)
                                                  (diagonal-default type))
        direction                             (direction diagonal-mode points origin-point)
        diagonal-top-left                     (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                    (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle-top-left                        (angle-to-point origin-point diagonal-top-left)
        angle-top-right                       (angle-to-point origin-point diagonal-top-right)
        line                                  (-> line
                                                  (update :offset max 0))
        {line-top-left        :line
         line-top-left-length :length}        (line/create line
                                                           (v/abs (v/- diagonal-top-left origin-point))
                                                           :angle (+ angle-top-left 180)
                                                           :reversed? true
                                                           :options options)
        {line-top-right :line}                (line/create line
                                                           (v/abs (v/- diagonal-top-right origin-point))
                                                           :angle angle-top-right
                                                           :flipped? true
                                                           :options options)
        {line-bottom :line}                   (line/create line
                                                           (v/abs (v/- bottom origin-point))
                                                           :flipped? true
                                                           :angle 90
                                                           :options options)
        {line-bottom-reversed        :line
         line-bottom-reversed-length :length} (line/create line
                                                           (v/abs (v/- bottom origin-point))
                                                           :angle -90
                                                           :reversed? true
                                                           :options options)
        diagonal-top-left-adjusted            (v/extend
                                                  origin-point
                                                diagonal-top-left
                                                line-top-left-length)
        bottom-adjusted                       (v/extend
                                                  origin-point
                                                bottom
                                                line-bottom-reversed-length)
        parts                                 [[["M" diagonal-top-left-adjusted
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :counter-clockwise
                                                                [:right :left]
                                                                [diagonal-top-right diagonal-top-left])
                                                 "z"]
                                                [diagonal-top-left origin-point diagonal-top-right]]

                                               [["M" bottom-adjusted
                                                 (line/stitch line-bottom-reversed)
                                                 "L" origin-point
                                                 (line/stitch line-top-right)
                                                 (infinity/path :clockwise
                                                                [:right :bottom]
                                                                [diagonal-top-right bottom])
                                                 "z"]
                                                [origin-point diagonal-top-right bottom-right bottom]]

                                               [["M" diagonal-top-left-adjusted
                                                 (line/stitch line-top-left)
                                                 "L" origin-point
                                                 (line/stitch line-bottom)
                                                 (infinity/path :clockwise
                                                                [:bottom :left]
                                                                [bottom diagonal-top-left])
                                                 "z"]
                                                [diagonal-top-left origin-point bottom bottom-left]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" bottom-adjusted
         (line/stitch line-bottom-reversed)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" diagonal-top-left-adjusted
                     (line/stitch line-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom)])}]])
     environment field top-level-render options :db-path db-path]))

(defn tierced-per-pairle-reversed [{:keys [type fields line origin hints] :as field} environment top-level-render options & {:keys [db-path]}]
  (let [points                             (:points environment)
        origin-point                       (point/calculate origin environment :fess)
        top                                (assoc (:top points) :x (:x origin-point))
        top-left                           (:top-left points)
        top-right                          (:top-right points)
        bottom-left                        (:bottom-left points)
        bottom-right                       (:bottom-right points)
        left                               (assoc (:left points) :y (:y origin-point))
        right                              (assoc (:right points) :y (:y origin-point))
        diagonal-mode                      (or (:diagonal-mode hints)
                                               (diagonal-default type))
        direction                          (direction diagonal-mode points origin-point)
        diagonal-bottom-left               (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                  (angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                 (angle-to-point origin-point diagonal-bottom-right)
        line                               (-> line
                                               (update :offset max 0))
        {line-bottom-right        :line
         line-bottom-right-length :length} (line/create line
                                                        (v/abs (v/- diagonal-bottom-right origin-point))
                                                        :angle (+ angle-bottom-right 180)
                                                        :reversed? true
                                                        :options options)
        {line-bottom-left :line}           (line/create line
                                                        (v/abs (v/- diagonal-bottom-left origin-point))
                                                        :angle angle-bottom-left
                                                        :flipped? true
                                                        :options options)
        {line-top :line}                   (line/create line
                                                        (v/abs (v/- top origin-point))
                                                        :flipped? true
                                                        :angle -90
                                                        :options options)
        {line-top-reversed        :line
         line-top-reversed-length :length} (line/create line
                                                        (v/abs (v/- top origin-point))
                                                        :angle 90
                                                        :reversed? true
                                                        :options options)
        diagonal-bottom-right-adjusted     (v/extend
                                               origin-point
                                             diagonal-bottom-right
                                             line-bottom-right-length)
        top-adjusted                       (v/extend
                                               origin-point
                                             top
                                             line-top-reversed-length)
        parts                              [[["M" top-adjusted
                                              (line/stitch line-top-reversed)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :clockwise
                                                             [:left :top]
                                                             [diagonal-bottom-left top-adjusted])
                                              "z"]
                                             [top-left top origin-point diagonal-bottom-left]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-top)
                                              (infinity/path :clockwise
                                                             [:top :right]
                                                             [top diagonal-bottom-right-adjusted])
                                              "z"]
                                             [top top-right diagonal-bottom-right origin-point]]

                                            [["M" diagonal-bottom-right-adjusted
                                              (line/stitch line-bottom-right)
                                              "L" origin-point
                                              (line/stitch line-bottom-left)
                                              (infinity/path :counter-clockwise
                                                             [:left :right]
                                                             [diagonal-bottom-left diagonal-bottom-right-adjusted])
                                              "z"]
                                             [origin-point bottom-left bottom-right]]]]
    [make-division
     (division-context-key type) fields parts
     [:all
      [(svg/make-path
        ["M" diagonal-bottom-right-adjusted
         (line/stitch line-bottom-right)])]
      nil]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-top)])}]
        [:path {:d (svg/make-path
                    ["M" diagonal-bottom-right-adjusted
                     (line/stitch line-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" origin-point
                     (line/stitch line-bottom-left)])}]])
     environment field top-level-render options :db-path db-path]))

(defn part-name [type index]
  (-> {:per-pale                    ["dexter" "sinister"]
       :per-fess                    ["chief" "base"]
       :per-bend                    ["chief" "base"]
       :per-bend-sinister           ["chief" "base"]
       :per-saltire                 ["chief" "dexter" "sinister" "base"]
       :per-chevron                 ["chief" "base"]
       :quarterly                   ["I" "II" "III" "IV"]
       :gyronny                     ["I" "II" "III" "IV" "V" "VI" "VII" "VIII"]
       :tierced-per-pale            ["dexter" "fess" "sinister"]
       :tierced-per-fess            ["chief" "fess" "base"]
       :tierced-per-pairle          ["chief" "dexter" "sinister"]
       :tierced-per-pairle-reversed ["dexter" "sinister" "base"]}
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
              [name key]))))

(defn render [{:keys [type] :as division} environment top-level-render options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    [function division environment top-level-render options :db-path db-path]))

(defn mandatory-part-count [type]
  (case type
    nil                          0
    :tierced-per-pale            3
    :tierced-per-fess            3
    :tierced-per-pairle          3
    :tierced-per-pairle-reversed 3
    2))

(defn counterchangable? [type]
  ;; TODO: potentially also should look at the parts, maybe demand no
  ;; ordinaries and charges as well, but for now this check suffices
  (-> type mandatory-part-count (= 2)))

(defn diagonal-options [type]
  (let [options {:forty-five-degrees "45°"
                 :top-left-fess      "Top-left to origin"
                 :top-right-fess     "Top-right to origin"
                 :bottom-left-fess   "Bottom-left to origin"
                 :bottom-right-fess  "Bottom-right to origin"}]
    (->> type
         (get {:per-bend                    [:forty-five-degrees
                                             :top-left-fess]
               :per-bend-sinister           [:forty-five-degrees
                                             :top-right-fess]
               :per-chevron                 [:forty-five-degrees
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :per-saltire                 [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :gyronny                     [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess
                                             :bottom-left-fess
                                             :bottom-right-fess]
               :tierced-per-pairle          [:forty-five-degrees
                                             :top-left-fess
                                             :top-right-fess]
               :tierced-per-pairle-reversed [:forty-five-degrees
                                             :bottom-left-fess
                                             :bottom-right-fess]})
         (map (fn [key]
                [(get options key) key])))))
