(ns or.coad.ordinary
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn thickness-default [type]
  (or (get {} type)
      30))

(defn thickness-options [type]
  (get {:pale [20 50]
        :fess [20 50]
        :chief [10 40]
        :base [10 40]
        :bend [10 40]} type))

(defn diagonal-options [type]
  (let [options {:forty-five-degrees "45°"
                 :top-left-fess "Top-left to fess"
                 :top-right-fess "Top-right to fess"
                 :bottom-left-fess "Bottom-left to fess"
                 :bottom-right-fess "Bottom-right to fess"}]
    (->> type
         (get {:bend [:forty-five-degrees
                      :top-left-fess]
               :bend-sinister [:forty-five-degrees
                               :top-right-fess]
               :chevron [:forty-five-degrees
                         :bottom-left-fess
                         :bottom-right-fess]})
         (map (fn [key]
                [(get options key) key])))))

(defn diagonal-default [type]
  (or (get {:bend-sinister :top-right-fess
            :chevron :forty-five-degrees} type)
      :top-left-fess))

(defn pale [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        top (:top points)
        bottom (:bottom points)
        fess (:fess points)
        width (:width environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-width (-> width
                       (* thickness)
                       (/ 100))
        col1 (- (:x fess) (/ band-width 2))
        col2 (+ col1 band-width)
        first-top (v/v col1 (:y top))
        first-bottom (v/v col1 (:y bottom))
        second-top (v/v col2 (:y top))
        second-bottom (v/v col2 (:y bottom))
        {line-one :line} (line/create line
                                      (:y (v/- bottom top))
                                      :flipped? true
                                      :angle 90
                                      :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true
                                                    :options options)
        second-bottom-adjusted (v/extend second-top second-bottom line-reversed-length)
        parts [[["M" first-top
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
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render options :db-path db-path]))

(defn fess [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        left (:left points)
        right (:right points)
        fess (:fess points)
        height (:height environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-height (-> height
                        (* thickness)
                        (/ 100))
        row1 (- (:y fess) (/ band-height 2))
        row2 (+ row1 band-height)
        first-left (v/v (:x left) row1)
        first-right (v/v (:x right) row1)
        second-left (v/v (:x left) row2)
        second-right (v/v (:x right) row2)
        {line-one :line} (line/create line
                                      (:x (v/- right left))
                                      :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        second-right-adjusted (v/extend second-left second-right line-reversed-length)
        parts [[["M" first-left
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
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [division/make-division
     :ordinary-fess [field] parts
     [:all]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render options :db-path db-path]))

(defn chief [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        top (:top points)
        top-left (:top-left points)
        left (:left points)
        right (:right points)
        height (:height environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-height (-> height
                        (* thickness)
                        (/ 100))
        row (+ (:y top) band-height)
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        {line-reversed :line
         line-reversed-length :length} (line/create line
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        row-right-adjusted (v/extend row-left row-right line-reversed-length)
        parts [[["M" row-right-adjusted
                 (line/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :right]
                                [row-left row-right-adjusted])
                 "z"]
                [top-left row-right]]]
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [division/make-division
     :ordinary-chief [field] parts
     [:all]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-right-adjusted
                     (line/stitch line-reversed)])}]])
     environment ordinary top-level-render options :db-path db-path]))

(defn base [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        bottom (:bottom points)
        bottom-right (:bottom-right points)
        left (:left points)
        right (:right points)
        height (:height environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-height (-> height
                        (* thickness)
                        (/ 100))
        row (- (:y bottom) band-height)
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        {line :line} (line/create line
                                  (:x (v/- right left))
                                  :options options)
        parts [[["M" row-left
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:right :left]
                                [row-right row-left])
                 "z"]
                [row-left bottom-right]]]
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [division/make-division
     :ordinary-base [field] parts
     [:all]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-left
                     (line/stitch line)])}]])
     environment ordinary top-level-render options :db-path db-path]))

(defn bend [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        left (:left points)
        right (:right points)
        fess (:fess points)
        height (:height environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-height (-> height
                        (* thickness)
                        (/ 100))
        diagonal-mode (or (:diagonal-mode hints)
                          (diagonal-default type))
        direction (division/direction diagonal-mode points)
        diagonal-start (v/project-x fess (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end (v/project-x fess (v/dot direction (v/v 1 1)) (:x right))
        angle (division/angle-to-point diagonal-start diagonal-end)
        line-length (v/abs (v/- diagonal-end diagonal-start))
        offset -40
        row1 (- (/ band-height 2))
        row2 (+ row1 band-height)
        first-left (v/v offset row1)
        first-right (v/v line-length row1)
        second-left (v/v offset row2)
        second-right (v/v line-length row2)
        {line-one :line} (line/create line
                                      line-length
                                      :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line
                                                    line-length
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        second-right-adjusted (v/extend second-left second-right line-reversed-length)
        parts [[["M" first-left
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
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [:g {:transform (str "translate(" (:x diagonal-start) "," (:y diagonal-start) ")"
                         "rotate(" angle ")")}
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (:outline? options)
        [:g.outline
         [:path {:d (svg/make-path
                     ["M" first-left
                      (line/stitch line)])}]
         [:path {:d (svg/make-path
                     ["M" second-right-adjusted
                      (line/stitch line-reversed)])}]])
      environment ordinary top-level-render options :db-path db-path]]))

(defn bend-sinister [{:keys [type field line hints] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [points (:points environment)
        left (:left points)
        right (:right points)
        fess (:fess points)
        height (:height environment)
        thickness (or (:thickness hints)
                      (thickness-default type))
        band-height (-> height
                        (* thickness)
                        (/ 100))
        diagonal-mode (or (:diagonal-mode hints)
                          (diagonal-default type))
        direction (division/direction diagonal-mode points)
        diagonal-start (v/project-x fess (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end (v/project-x fess (v/dot direction (v/v 1 -1)) (:x right))
        angle (division/angle-to-point diagonal-start diagonal-end)
        line-length (v/abs (v/- diagonal-end diagonal-start))
        row1 (- (/ band-height 2))
        row2 (+ row1 band-height)
        offset -40
        first-left (v/v offset row1)
        first-right (v/v line-length row1)
        second-left (v/v offset row2)
        second-right (v/v line-length row2)
        {line-one :line} (line/create line
                                      line-length
                                      :options options)
        {line-reversed :line
         line-reversed-length :length} (line/create line
                                                    line-length
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180
                                                    :options options)
        second-right-adjusted (v/extend second-left second-right line-reversed-length)
        parts [[["M" first-left
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
        field (if (charge/counterchangable? field parent)
                (charge/counterchange-field field parent)
                field)]
    [:g {:transform (str "translate(" (:x diagonal-start) "," (:y diagonal-start) ")"
                         "rotate(" angle ")")}
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (:outline? options)
        [:g.outline
         [:path {:d (svg/make-path
                     ["M" first-left
                      (line/stitch line)])}]
         [:path {:d (svg/make-path
                     ["M" second-right-adjusted
                      (line/stitch line-reversed)])}]])
      environment ordinary top-level-render options :db-path db-path]]))

(def kinds
  [["Pale" :pale pale]
   ["Fess" :fess fess]
   ["Chief" :chief chief]
   ["Base" :base base]
   ["Bend" :bend bend]
   ["Bend Sinister" :bend-sinister bend-sinister]
   ;; ["Cross" :cross cross]
   ;; ["Saltire" :saltire saltire]
   ;; ["Chevron" :chevron chevron]
   ;; ["Pall" :pall pall]
   ])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [name key]))))

(defn render [{:keys [type] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    [function ordinary parent environment top-level-render options :db-path db-path]))
