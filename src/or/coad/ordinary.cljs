(ns or.coad.ordinary
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.position :as position]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn thickness-default [type]
  (or (get {} type)
      30))

(defn thickness-options [type]
  (get {:pale          [20 50]
        :fess          [20 50]
        :chief         [10 40]
        :base          [10 40]
        :bend          [10 40]
        :bend-sinister [10 40]
        :cross         [10 30]
        :saltire       [10 30]
        :chevron       [10 30]
        :pall          [10 30]} type))

(defn diagonal-options [type]
  (let [options {:forty-five-degrees "45°"
                 :top-left-fess      "Top-left to fess"
                 :top-right-fess     "Top-right to fess"
                 :bottom-left-fess   "Bottom-left to fess"
                 :bottom-right-fess  "Bottom-right to fess"}]
    (->> type
         (get {:bend          [:forty-five-degrees
                               :top-left-fess]
               :bend-sinister [:forty-five-degrees
                               :top-right-fess]
               :chevron       [:forty-five-degrees
                               :bottom-left-fess
                               :bottom-right-fess]
               :saltire       [:forty-five-degrees
                               :top-left-fess
                               :top-right-fess
                               :bottom-left-fess
                               :bottom-right-fess]})
         (map (fn [key]
                [(get options key) key])))))

(defn diagonal-default [type]
  (or (get {:bend-sinister :top-right-fess
            :chevron       :forty-five-degrees} type)
      :top-left-fess))

(defn pale
  {:display-name "Pale"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top                            (assoc (:top points) :x (:x origin-point))
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        width                          (:width environment)
        thickness                      (or (:thickness hints)
                                           (thickness-default type))
        band-width                     (-> width
                                           (* thickness)
                                           (/ 100))
        col1                           (- (:x origin-point) (/ band-width 2))
        col2                           (+ col1 band-width)
        first-top                      (v/v col1 (:y top))
        first-bottom                   (v/v col1 (:y bottom))
        second-top                     (v/v col2 (:y top))
        second-bottom                  (v/v col2 (:y bottom))
        {line-one :line}               (line/create line
                                                    (:y (v/- bottom top))
                                                    :flipped? true
                                                    :angle 90
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :flipped? true
                                                    :reversed? true
                                                    :render-options render-options)
        second-bottom-adjusted         (v/extend second-top second-bottom line-reversed-length)
        parts                          [[["M" first-top
                                          (line/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:bottom :bottom]
                                                         [first-bottom second-bottom-adjusted])
                                          (line/stitch line-reversed)
                                          (infinity/path :counter-clockwise
                                                         [:top :top]
                                                         [second-top first-top])
                                          "z"]
                                         [first-top second-bottom]]]
        field                          (if (charge/counterchangable? field parent)
                                         (charge/counterchange-field field parent)
                                         field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn fess
  {:display-name "Fess"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        height                         (:height environment)
        thickness                      (or (:thickness hints)
                                           (thickness-default type))
        band-height                    (-> height
                                           (* thickness)
                                           (/ 100))
        row1                           (- (:y origin-point) (/ band-height 2))
        row2                           (+ row1 band-height)
        first-left                     (v/v (:x left) row1)
        first-right                    (v/v (:x right) row1)
        second-left                    (v/v (:x left) row2)
        second-right                   (v/v (:x right) row2)
        {line-one :line}               (line/create line
                                                    (:x (v/- right left))
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        parts                          [[["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :right]
                                                         [first-right second-right-adjusted])
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:left :left]
                                                         [second-left first-left])
                                          "z"]
                                         [first-right second-left]]]
        field                          (if (charge/counterchangable? field parent)
                                         (charge/counterchange-field field parent)
                                         field)]
    [division/make-division
     :ordinary-fess [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn chief
  {:display-name "Chief"}
  [{:keys [type field line hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                         (:points environment)
        top                            (:top points)
        top-left                       (:top-left points)
        left                           (:left points)
        right                          (:right points)
        height                         (:height environment)
        thickness                      (or (:thickness hints)
                                           (thickness-default type))
        band-height                    (-> height
                                           (* thickness)
                                           (/ 100))
        row                            (+ (:y top) band-height)
        row-left                       (v/v (:x left) row)
        row-right                      (v/v (:x right) row)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        row-right-adjusted             (v/extend row-left row-right line-reversed-length)
        parts                          [[["M" row-right-adjusted
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:left :right]
                                                         [row-left row-right-adjusted])
                                          "z"]
                                         [top-left row-right]]]
        field                          (if (charge/counterchangable? field parent)
                                         (charge/counterchange-field field parent)
                                         field)]
    [division/make-division
     :ordinary-chief [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn base
  {:display-name "Base"}
  [{:keys [type field line hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points           (:points environment)
        bottom           (:bottom points)
        bottom-right     (:bottom-right points)
        left             (:left points)
        right            (:right points)
        height           (:height environment)
        thickness        (or (:thickness hints)
                             (thickness-default type))
        band-height      (-> height
                             (* thickness)
                             (/ 100))
        row              (- (:y bottom) band-height)
        row-left         (v/v (:x left) row)
        row-right        (v/v  (:x right) row)
        {line-one :line} (line/create line
                                      (:x (v/- right left))
                                      :render-options render-options)
        parts            [[["M" row-left
                            (line/stitch line-one)
                            (infinity/path :clockwise
                                           [:right :left]
                                           [row-right row-left])
                            "z"]
                           [row-left bottom-right]]]
        field            (if (charge/counterchangable? field parent)
                           (charge/counterchange-field field parent)
                           field)]
    [division/make-division
     :ordinary-base [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-left
                     (line/stitch line-one)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn bend
  {:display-name "Bend"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        height                         (:height environment)
        thickness                      (or (:thickness hints)
                                           (thickness-default type))
        band-height                    (-> height
                                           (* thickness)
                                           (/ 100))
        diagonal-mode                  (or (:diagonal-mode hints)
                                           (diagonal-default type))
        direction                      (division/direction diagonal-mode points origin-point)
        diagonal-start                 (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end                   (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle                          (division/angle-to-point diagonal-start diagonal-end)
        line-length                    (v/abs (v/- diagonal-end diagonal-start))
        offset                         -40
        row1                           (- (/ band-height 2))
        row2                           (+ row1 band-height)
        first-left                     (v/v offset row1)
        first-right                    (v/v line-length row1)
        second-left                    (v/v offset row2)
        second-right                   (v/v line-length row2)
        {line-one :line}               (line/create line
                                                    line-length
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    line-length
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        parts                          [[["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :right]
                                                         [first-right second-right-adjusted])
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:left :left]
                                                         [second-left first-left])
                                          "z"]
                                         [first-right second-left]]]
        field                          (if (charge/counterchangable? field parent)
                                         (charge/counterchange-field field parent)
                                         field)]
    [:g {:transform (str "translate(" (:x diagonal-start) "," (:y diagonal-start) ")"
                         "rotate(" angle ")")}
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (:outline? render-options)
        [:g.outline
         [:path {:d (svg/make-path
                     ["M" first-left
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" second-right-adjusted
                      (line/stitch line-reversed)])}]])
      environment ordinary top-level-render render-options :db-path db-path]]))

(defn bend-sinister
  {:display-name "Bend sinister"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        left                           (assoc (:left points) :y (:y origin-point))
        right                          (assoc (:right points) :y (:y origin-point))
        height                         (:height environment)
        thickness                      (or (:thickness hints)
                                           (thickness-default type))
        band-height                    (-> height
                                           (* thickness)
                                           (/ 100))
        diagonal-mode                  (or (:diagonal-mode hints)
                                           (diagonal-default type))
        direction                      (division/direction diagonal-mode points origin-point)
        diagonal-start                 (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end                   (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle                          (division/angle-to-point diagonal-start diagonal-end)
        line-length                    (v/abs (v/- diagonal-end diagonal-start))
        row1                           (- (/ band-height 2))
        row2                           (+ row1 band-height)
        offset                         -40
        first-left                     (v/v offset row1)
        first-right                    (v/v line-length row1)
        second-left                    (v/v offset row2)
        second-right                   (v/v line-length row2)
        {line-one :line}               (line/create line
                                                    line-length
                                                    :render-options render-options)
        {line-reversed        :line
         line-reversed-length :length} (line/create line
                                                    line-length
                                                    :reversed? true
                                                    :angle 180
                                                    :render-options render-options)
        second-right-adjusted          (v/extend second-left second-right line-reversed-length)
        parts                          [[["M" first-left
                                          (line/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:right :right]
                                                         [first-right second-right-adjusted])
                                          (line/stitch line-reversed)
                                          (infinity/path :clockwise
                                                         [:left :left]
                                                         [second-left first-left])
                                          "z"]
                                         [first-right second-left]]]
        field                          (if (charge/counterchangable? field parent)
                                         (charge/counterchange-field field parent)
                                         field)]
    [:g {:transform (str "translate(" (:x diagonal-start) "," (:y diagonal-start) ")"
                         "rotate(" angle ")")}
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (:outline? render-options)
        [:g.outline
         [:path {:d (svg/make-path
                     ["M" first-left
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" second-right-adjusted
                      (line/stitch line-reversed)])}]])
      environment ordinary top-level-render render-options :db-path db-path]]))

(defn cross
  {:display-name "Cross"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                                  (:points environment)
        origin-point                            (position/calculate origin environment :fess)
        top                                     (assoc (:top points) :x (:x origin-point))
        bottom                                  (assoc (:bottom points) :x (:x origin-point))
        left                                    (assoc (:left points) :y (:y origin-point))
        right                                   (assoc (:right points) :y (:y origin-point))
        width                                   (:width environment)
        thickness                               (or (:thickness hints)
                                                    (thickness-default type))
        band-width                              (-> width
                                                    (* thickness)
                                                    (/ 100))
        col1                                    (- (:x origin-point) (/ band-width 2))
        col2                                    (+ col1 band-width)
        pale-top-left                           (v/v col1 (:y top))
        pale-bottom-left                        (v/v col1 (:y bottom))
        pale-top-right                          (v/v col2 (:y top))
        pale-bottom-right                       (v/v col2 (:y bottom))
        row1                                    (- (:y origin-point) (/ band-width 2))
        row2                                    (+ row1 band-width)
        fess-top-left                           (v/v (:x left) row1)
        fess-top-right                          (v/v (:x right) row1)
        fess-bottom-left                        (v/v (:x left) row2)
        fess-bottom-right                       (v/v (:x right) row2)
        corner-top-left                         (v/v col1 row1)
        corner-top-right                        (v/v col2 row1)
        corner-bottom-left                      (v/v col1 row2)
        corner-bottom-right                     (v/v col2 row2)
        line                                    (-> line
                                                    (update :offset max 0))
        {line-pale-top-left :line}              (line/create line
                                                             (v/abs (v/- corner-top-left pale-top-left))
                                                             :angle     -90
                                                             :reversed? true
                                                             :render-options   render-options)
        {line-pale-top-right        :line
         line-pale-top-right-length :length}    (line/create line
                                                             (v/abs (v/- corner-top-right pale-top-right))
                                                             :angle 90
                                                             :render-options render-options)
        {line-fess-top-right :line}             (line/create line
                                                             (v/abs (v/- corner-top-right fess-top-right))
                                                             :reversed? true
                                                             :render-options render-options)
        {line-fess-bottom-right        :line
         line-fess-bottom-right-length :length} (line/create line
                                                             (v/abs (v/- corner-bottom-right fess-bottom-right))
                                                             :angle 180
                                                             :render-options render-options)
        {line-pale-bottom-right :line}          (line/create line
                                                             (v/abs (v/- corner-bottom-right pale-bottom-right))
                                                             :angle 90
                                                             :reversed? true
                                                             :render-options render-options)
        {line-pale-bottom-left        :line
         line-pale-bottom-left-length :length}  (line/create line
                                                             (v/abs (v/- corner-bottom-left pale-bottom-left))
                                                             :angle -90
                                                             :render-options render-options)
        {line-fess-bottom-left :line}           (line/create line
                                                             (v/abs (v/- corner-bottom-left fess-bottom-left))
                                                             :angle 180
                                                             :reversed? true
                                                             :render-options render-options)
        {line-fess-top-left        :line
         line-fess-top-left-length :length}     (line/create line
                                                             (v/abs (v/- corner-top-left fess-top-left))
                                                             :render-options render-options)
        pale-top-right-adjusted                 (v/extend corner-top-right pale-top-right
                                                          line-pale-top-right-length)
        fess-bottom-right-adjusted              (v/extend corner-bottom-right fess-bottom-right
                                                          line-fess-bottom-right-length)
        pale-bottom-left-adjusted               (v/extend corner-bottom-left pale-bottom-left
                                                          line-pale-bottom-left-length)
        fess-top-left-adjusted                  (v/extend corner-top-left fess-top-left
                                                          line-fess-top-left-length)
        parts                                   [[["M" corner-top-left
                                                   (line/stitch line-pale-top-left)
                                                   (infinity/path :clockwise
                                                                  [:top :top]
                                                                  [pale-top-left pale-top-right-adjusted])
                                                   (line/stitch line-pale-top-right)
                                                   "L" corner-top-right
                                                   (line/stitch line-fess-top-right)
                                                   (infinity/path :clockwise
                                                                  [:right :right]
                                                                  [fess-top-right fess-bottom-right-adjusted])
                                                   (line/stitch line-fess-bottom-right)
                                                   "L" corner-bottom-right
                                                   (line/stitch line-pale-bottom-right)
                                                   (infinity/path :clockwise
                                                                  [:bottom :bottom]
                                                                  [pale-bottom-right pale-bottom-left-adjusted])
                                                   (line/stitch line-pale-bottom-left)
                                                   "L" corner-bottom-left
                                                   (line/stitch line-fess-bottom-left)
                                                   (infinity/path :clockwise
                                                                  [:left :left]
                                                                  [fess-bottom-left fess-top-left-adjusted])
                                                   (line/stitch line-fess-top-left)
                                                   "z"]
                                                  [top bottom left right]]]]
    field                          (if (charge/counterchangable? field parent)
                                     (charge/counterchange-field field parent)
                                     field)
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" corner-top-left
                     (line/stitch line-pale-top-left)])}]
        [:path {:d (svg/make-path
                    ["M" pale-top-right-adjusted
                     (line/stitch line-pale-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" corner-top-right
                     (line/stitch line-fess-top-right)])}]
        [:path {:d (svg/make-path
                    ["M" fess-bottom-right-adjusted
                     (line/stitch line-fess-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" corner-bottom-right
                     (line/stitch line-pale-bottom-right)])}]
        [:path {:d (svg/make-path
                    ["M" pale-bottom-left-adjusted
                     (line/stitch line-pale-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" corner-bottom-left
                     (line/stitch line-fess-bottom-left)])}]
        [:path {:d (svg/make-path
                    ["M" fess-top-left-adjusted
                     (line/stitch line-fess-top-left)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn saltire
  {:display-name "Saltire"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        top                                      (assoc (:top points) :x (:x origin-point))
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        left                                     (assoc (:left points) :y (:y origin-point))
        right                                    (assoc (:right points) :y (:y origin-point))
        width                                    (:width environment)
        thickness                                (or (:thickness hints)
                                                     (thickness-default type))
        band-width                               (-> width
                                                     (* thickness)
                                                     (/ 100))
        diagonal-mode                            (or (:diagonal-mode hints)
                                                     (diagonal-default type))
        direction                                (division/direction diagonal-mode points origin-point)
        diagonal-top-left                        (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                       (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left                     (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right                    (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left                           (division/angle-to-point origin-point diagonal-top-left)
        angle-top-right                          (division/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                        (division/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                       (division/angle-to-point origin-point diagonal-bottom-right)
        angle                                    (-> angle-bottom-right (* Math/PI) (/ 180))
        dx                                       (/ band-width 2 (Math/sin angle))
        dy                                       (/ band-width 2 (Math/cos angle))
        offset-top                               (v/v 0 (- dy))
        offset-bottom                            (v/v 0 dy)
        offset-left                              (v/v (- dx) 0)
        offset-right                             (v/v dx 0)
        corner-top                               (v/+ origin-point offset-top)
        corner-bottom                            (v/+ origin-point offset-bottom)
        corner-left                              (v/+ origin-point offset-left)
        corner-right                             (v/+ origin-point offset-right)
        top-left-upper                           (v/+ diagonal-top-left offset-top)
        top-left-lower                           (v/+ diagonal-top-left offset-bottom)
        top-right-upper                          (v/+ diagonal-top-right offset-top)
        top-right-lower                          (v/+ diagonal-top-right offset-bottom)
        bottom-left-upper                        (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                        (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                       (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                       (v/+ diagonal-bottom-right offset-bottom)
        line                                     (-> line
                                                     (update :offset max 0))
        {line-top-left-lower :line}              (line/create line
                                                              (Math/abs (:y (v/- corner-left top-left-lower)))
                                                              :angle  angle-top-left
                                                              :reversed? true
                                                              :render-options   render-options)
        {line-top-left-upper        :line
         line-top-left-upper-length :length}     (line/create line
                                                              (v/abs (v/- corner-top top-left-upper))
                                                              :angle     (- angle-top-left 180)
                                                              :render-options render-options)
        {line-top-right-upper :line}             (line/create line
                                                              (v/abs (v/- corner-top top-right-upper))
                                                              :reversed? true
                                                              :angle angle-top-right
                                                              :render-options render-options)
        {line-top-right-lower        :line
         line-top-right-lower-length :length}    (line/create line
                                                              (v/abs (v/- corner-right top-right-lower))
                                                              :angle (- angle-top-right 180)
                                                              :render-options render-options)
        {line-bottom-right-upper :line}          (line/create line
                                                              (v/abs (v/- corner-right bottom-right-upper))
                                                              :angle angle-bottom-right
                                                              :reversed? true
                                                              :render-options render-options)
        {line-bottom-right-lower        :line
         line-bottom-right-lower-length :length} (line/create line
                                                              (v/abs (v/- corner-bottom bottom-right-lower))
                                                              :angle (- angle-bottom-right 180)
                                                              :render-options render-options)
        {line-bottom-left-lower :line}           (line/create line
                                                              (v/abs (v/- corner-bottom bottom-left-lower))
                                                              :angle angle-bottom-left
                                                              :reversed? true
                                                              :render-options render-options)
        {line-bottom-left-upper        :line
         line-bottom-left-upper-length :length}  (line/create line
                                                              (v/abs (v/- corner-left bottom-left-upper))
                                                              :angle (- angle-bottom-left 180)
                                                              :render-options render-options)
        top-left-upper-adjusted                  (v/extend corner-top top-left-upper
                                                           line-top-left-upper-length)
        top-right-lower-adjusted                 (v/extend corner-right top-right-lower
                                                           line-top-right-lower-length)
        bottom-right-lower-adjusted              (v/extend corner-bottom bottom-right-lower
                                                           line-bottom-right-lower-length)
        bottom-left-upper-adjusted               (v/extend corner-left bottom-left-upper
                                                           line-bottom-left-upper-length)
        parts                                    [[["M" corner-left
                                                    (line/stitch line-top-left-lower)
                                                    (infinity/path :clockwise
                                                                   [:left :left]
                                                                   [top-left-lower top-left-upper-adjusted])
                                                    (line/stitch line-top-left-upper)
                                                    "L" corner-top
                                                    (line/stitch line-top-right-upper)
                                                    (infinity/path :clockwise
                                                                   [:right :right]
                                                                   [top-right-upper top-right-lower-adjusted])
                                                    (line/stitch line-top-right-lower)
                                                    "L" corner-right
                                                    (line/stitch line-bottom-right-upper)
                                                    (infinity/path :clockwise
                                                                   [:right :right]
                                                                   [bottom-right-upper bottom-right-lower-adjusted])
                                                    (line/stitch line-bottom-right-lower)
                                                    "L" corner-bottom
                                                    (line/stitch line-bottom-left-lower)
                                                    (infinity/path :clockwise
                                                                   [:left :left]
                                                                   [bottom-left-lower bottom-left-upper-adjusted])
                                                    (line/stitch line-bottom-left-upper)
                                                    "z"]
                                                   [top bottom left right]]]]
    field                          (if (charge/counterchangable? field parent)
                                     (charge/counterchange-field field parent)
                                     field)
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" corner-left
                     (line/stitch line-top-left-lower)])}]
        [:path {:d (svg/make-path
                    ["M" top-left-upper-adjusted
                     (line/stitch line-top-left-upper)])}]
        [:path {:d (svg/make-path
                    ["M" corner-top
                     (line/stitch line-top-right-upper)])}]
        [:path {:d (svg/make-path
                    ["M" top-right-lower-adjusted
                     (line/stitch line-top-right-lower)])}]
        [:path {:d (svg/make-path
                    ["M" corner-right
                     (line/stitch line-bottom-right-upper)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-right-lower-adjusted
                     (line/stitch line-bottom-right-lower)])}]
        [:path {:d (svg/make-path
                    ["M" corner-bottom
                     (line/stitch line-bottom-left-lower)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-left-upper-adjusted
                     (line/stitch line-bottom-left-upper)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(defn chevron
  {:display-name "Chevron"}
  [{:keys [type field line origin hints] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [points                                   (:points environment)
        origin-point                             (position/calculate origin environment :fess)
        top                                      (assoc (:top points) :x (:x origin-point))
        bottom                                   (assoc (:bottom points) :x (:x origin-point))
        left                                     (assoc (:left points) :y (:y origin-point))
        right                                    (assoc (:right points) :y (:y origin-point))
        height                                   (:height environment)
        thickness                                (or (:thickness hints)
                                                     (thickness-default type))
        band-width                               (-> height
                                                     (* thickness)
                                                     (/ 100))
        diagonal-mode                            (or (:diagonal-mode hints)
                                                     (diagonal-default type))
        direction                                (division/direction diagonal-mode points origin-point)
        diagonal-bottom-left                     (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right                    (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                        (division/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                       (division/angle-to-point origin-point diagonal-bottom-right)
        angle                                    (-> angle-bottom-right (* Math/PI) (/ 180))
        dy                                       (/ band-width 2 (Math/cos angle))
        offset-top                               (v/v 0 (- dy))
        offset-bottom                            (v/v 0 dy)
        corner-top                               (v/+ origin-point offset-top)
        corner-bottom                            (v/+ origin-point offset-bottom)
        bottom-left-upper                        (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                        (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                       (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                       (v/+ diagonal-bottom-right offset-bottom)
        line                                     (-> line
                                                     (update :offset max 0))
        {line-bottom-right-upper :line}          (line/create line
                                                              (v/abs (v/- corner-top bottom-right-upper))
                                                              :angle angle-bottom-right
                                                              :render-options render-options)
        {line-bottom-right-lower        :line
         line-bottom-right-lower-length :length} (line/create line
                                                              (v/abs (v/- corner-bottom bottom-right-lower))
                                                              :angle (- angle-bottom-right 180)
                                                              :reversed? true
                                                              :render-options render-options)
        {line-bottom-left-lower :line}           (line/create line
                                                              (v/abs (v/- corner-bottom bottom-left-lower))
                                                              :angle angle-bottom-left
                                                              :render-options render-options)
        {line-bottom-left-upper        :line
         line-bottom-left-upper-length :length}  (line/create line
                                                              (v/abs (v/- corner-top bottom-left-upper))
                                                              :angle (- angle-bottom-left 180)
                                                              :reversed? true
                                                              :render-options render-options)
        bottom-right-lower-adjusted              (v/extend corner-bottom bottom-right-lower
                                                           line-bottom-right-lower-length)
        bottom-left-upper-adjusted               (v/extend corner-top bottom-left-upper
                                                           line-bottom-left-upper-length)
        parts                                    [[["M" corner-top
                                                    (line/stitch line-bottom-right-upper)
                                                    (infinity/path :clockwise
                                                                   [:right :right]
                                                                   [bottom-right-upper bottom-right-lower-adjusted])
                                                    (line/stitch line-bottom-right-lower)
                                                    "L" corner-bottom
                                                    (line/stitch line-bottom-left-lower)
                                                    (infinity/path :clockwise
                                                                   [:left :left]
                                                                   [bottom-left-lower bottom-left-upper-adjusted])
                                                    (line/stitch line-bottom-left-upper)
                                                    "z"]
                                                   [top bottom left right]]]]
    field                          (if (charge/counterchangable? field parent)
                                     (charge/counterchange-field field parent)
                                     field)
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (:outline? render-options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" corner-top
                     (line/stitch line-bottom-right-upper)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-right-lower-adjusted
                     (line/stitch line-bottom-right-lower)])}]
        [:path {:d (svg/make-path
                    ["M" corner-bottom
                     (line/stitch line-bottom-left-lower)])}]
        [:path {:d (svg/make-path
                    ["M" bottom-left-upper-adjusted
                     (line/stitch line-bottom-left-upper)])}]])
     environment ordinary top-level-render render-options :db-path db-path]))

(def ordinaries
  [#'pale
   #'fess
   #'chief
   #'base
   #'bend
   #'bend-sinister
   #'cross
   #'saltire
   #'chevron])

(def kinds-function-map
  (->> ordinaries
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> ordinaries
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(defn render [{:keys [type] :as ordinary} parent environment top-level-render render-options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    [function ordinary parent environment top-level-render render-options :db-path db-path]))
