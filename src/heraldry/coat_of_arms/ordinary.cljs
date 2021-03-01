(ns heraldry.coat-of-arms.ordinary
  (:require [heraldry.coat-of-arms.charge :as charge]
            [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.division.core :as division]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn diagonal-mode-choices [type]
  (let [options {:forty-five-degrees  "45°"
                 :top-left-origin     "Top-left to origin"
                 :top-right-origin    "Top-right to origin"
                 :bottom-left-origin  "Bottom-left to origin"
                 :bottom-right-origin "Bottom-right to origin"}]
    (->> type
         (get {:bend          [:forty-five-degrees
                               :top-left-origin]
               :bend-sinister [:forty-five-degrees
                               :top-right-origin]
               :chevron       [:forty-five-degrees
                               :bottom-left-origin
                               :bottom-right-origin]
               :saltire       [:forty-five-degrees
                               :top-left-origin
                               :top-right-origin
                               :bottom-left-origin
                               :bottom-right-origin]})
         (map (fn [key]
                [(get options key) key])))))

(defn diagonal-default [type]
  (or (get {:bend-sinister :top-right-origin
            :chevron       :forty-five-degrees} type)
      :top-left-origin))

(def default-options
  {:origin        position/default-options
   :diagonal-mode {:type    :choice
                   :default :top-left-origin}
   :line          line/default-options
   :opposite-line line/default-options
   :geometry      (-> geometry/default-options
                      (assoc-in [:size :min] 10)
                      (assoc-in [:size :default] 25)
                      (assoc :mirrored? nil)
                      (assoc :reversed? nil)
                      (assoc :stretch nil)
                      (assoc :rotation nil))})

(defn options [ordinary]
  (when ordinary
    (let [type (:type ordinary)]
      (->
       default-options
       (options/merge {:line (line/options (get-in ordinary [:line]))})
       (options/merge {:opposite-line (line/options (get-in ordinary [:opposite-line]))})
       (options/merge
        (->
         (get {:pale          {:origin        {:offset-y nil}
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :fess          {:origin        {:offset-x nil}
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :chief         {:origin        nil
                               :diagonal-mode nil
                               :opposite-line nil
                               :geometry      {:size {:max 50}}}
               :base          {:origin        nil
                               :opposite-line nil
                               :diagonal-mode nil
                               :geometry      {:size {:max 50}}}
               :bend          {:origin        {:point {:choices position/point-choices-y}}
                               :diagonal-mode {:choices (diagonal-mode-choices
                                                         :bend)}
                               :geometry      {:size {:max 50}}}
               :bend-sinister {:origin        {:point {:choices position/point-choices-y}}
                               :diagonal-mode {:choices (diagonal-mode-choices
                                                         :bend-sinister)
                                               :default :top-right-origin}
                               :geometry      {:size {:max 50}}}
               :chevron       {:diagonal-mode {:choices (diagonal-mode-choices
                                                         :chevron)
                                               :default :forty-five-degrees}
                               :line          {:offset {:min 0}}
                               :opposite-line {:offset {:min 0}}
                               :geometry      {:size {:max 30}}}
               :saltire       {:diagonal-mode {:choices (diagonal-mode-choices
                                                         :saltire)}
                               :line          {:offset {:min 0}}
                               :opposite-line nil
                               :geometry      {:size {:max 30}}}
               :cross         {:diagonal-mode nil
                               :line          {:offset {:min 0}}
                               :opposite-line nil
                               :geometry      {:size {:max 30}}}}
              type)))))))

(defn sanitize-opposite-line [ordinary line]
  (-> (options/sanitize
       (util/deep-merge-with (fn [_current-value new-value]
                               new-value) line
                             (into {}
                                   (filter (fn [[_ v]]
                                             (some? v))
                                           (:opposite-line ordinary))))
       (-> ordinary options :opposite-line))
      (assoc :flipped? (if (-> ordinary :opposite-line :flipped?)
                         (not (:flipped? line))
                         (:flipped? line)))))

(defn percent-of [base-value]
  (fn [v]
    (when v
      (-> v
          (* base-value)
          (/ 100)))))

(defn pale
  {:display-name "Pale"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                          (options/sanitize ordinary (options ordinary))
        opposite-line                                           (sanitize-opposite-line ordinary line)
        {:keys [size]}                                          geometry
        points                                                  (:points environment)
        origin-point                                            (position/calculate origin environment :fess)
        top                                                     (assoc (:top points) :x (:x origin-point))
        bottom                                                  (assoc (:bottom points) :x (:x origin-point))
        width                                                   (:width environment)
        band-width                                              (-> width
                                                                    (* size)
                                                                    (/ 100))
        col1                                                    (- (:x origin-point) (/ band-width 2))
        col2                                                    (+ col1 band-width)
        first-top                                               (v/v col1 (:y top))
        first-bottom                                            (v/v col1 (:y bottom))
        second-top                                              (v/v col2 (:y top))
        second-bottom                                           (v/v col2 (:y bottom))
        line                                                    (-> line
                                                                    (update-in [:fimbriation :thickness-1] (percent-of width))
                                                                    (update-in [:fimbriation :thickness-2] (percent-of width)))
        opposite-line                                           (-> opposite-line
                                                                    (update-in [:fimbriation :thickness-1] (percent-of width))
                                                                    (update-in [:fimbriation :thickness-2] (percent-of width)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}                          (line/create line
                                                                             (:y (v/- bottom top))
                                                                             :angle -90
                                                                             :reversed? true
                                                                             :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}                (line/create opposite-line
                                                                             (:y (v/- bottom top))
                                                                             :angle 90
                                                                             :render-options render-options)
        parts                                                   [[["M" (v/+ first-bottom
                                                                            line-one-start)
                                                                   (svg/stitch line-one)
                                                                   (infinity/path :clockwise
                                                                                  [:top :top]
                                                                                  [(v/+ first-top
                                                                                        line-one-start)
                                                                                   (v/+ second-top
                                                                                        line-reversed-start)])
                                                                   (svg/stitch line-reversed)
                                                                   (infinity/path :clockwise
                                                                                  [:bottom :bottom]
                                                                                  [(v/+ second-bottom
                                                                                        line-reversed-start)
                                                                                   (v/+ first-bottom
                                                                                        line-one-start)])
                                                                   "z"]
                                                                  [(v/+ first-bottom
                                                                        line-one-start)
                                                                   (v/+ second-top
                                                                        line-reversed-start)]]]
        field                                                   (if (charge/counterchangable? field parent)
                                                                  (charge/counterchange-field field parent)
                                                                  field)
        [fimbriation-elements-left fimbriation-outlines-left]   (fimbriation/render
                                                                 [first-bottom :bottom]
                                                                 [first-top :top]
                                                                 [line-one-data]
                                                                 (:fimbriation line)
                                                                 render-options)
        [fimbriation-elements-right fimbriation-outlines-right] (fimbriation/render
                                                                 [second-top :top]
                                                                 [second-bottom :bottom]
                                                                 [line-reversed-data]
                                                                 (:fimbriation opposite-line)
                                                                 render-options)]
    [:<>
     fimbriation-elements-left
     fimbriation-elements-right
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-bottom
                               line-one-start)
                      (svg/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-top
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines-left
         fimbriation-outlines-right])
      environment ordinary context]]))

(defn fess
  {:display-name "Fess"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                            (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                            geometry
        opposite-line                                             (sanitize-opposite-line ordinary line)
        points                                                    (:points environment)
        origin-point                                              (position/calculate origin environment :fess)
        left                                                      (assoc (:left points) :y (:y origin-point))
        right                                                     (assoc (:right points) :y (:y origin-point))
        height                                                    (:height environment)
        band-height                                               (-> height
                                                                      (* size)
                                                                      (/ 100))
        row1                                                      (- (:y origin-point) (/ band-height 2))
        row2                                                      (+ row1 band-height)
        line                                                      (-> line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                             (-> opposite-line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}                            (line/create line
                                                                               (:x (v/- right left))
                                                                               :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}                  (line/create opposite-line
                                                                               (:x (v/- right left))
                                                                               :reversed? true
                                                                               :angle 180
                                                                               :render-options render-options)
        first-left                                                (v/v (:x left) row1)
        first-right                                               (v/v (:x right) row1)
        second-left                                               (v/v (:x left) row2)
        second-right                                              (v/v (:x right) row2)
        parts                                                     [[["M" (v/+ first-left
                                                                              line-one-start)
                                                                     (svg/stitch line-one)
                                                                     (infinity/path :clockwise
                                                                                    [:right :right]
                                                                                    [(v/+ first-right
                                                                                          line-one-start)
                                                                                     (v/+ second-right
                                                                                          line-reversed-start)])
                                                                     (svg/stitch line-reversed)
                                                                     (infinity/path :clockwise
                                                                                    [:left :left]
                                                                                    [(v/+ second-left
                                                                                          line-reversed-start)
                                                                                     (v/+ first-left
                                                                                          line-one-start)])
                                                                     "z"]
                                                                    [(v/+ first-right
                                                                          line-one-start)
                                                                     (v/+ second-left
                                                                          line-reversed-start)]]]
        field                                                     (if (charge/counterchangable? field parent)
                                                                    (charge/counterchange-field field parent)
                                                                    field)
        [fimbriation-elements-top fimbriation-outlines-top]       (fimbriation/render
                                                                   [first-left :left]
                                                                   [first-right :right]
                                                                   [line-one-data]
                                                                   (:fimbriation line)
                                                                   render-options)
        [fimbriation-elements-bottom fimbriation-outlines-bottom] (fimbriation/render
                                                                   [second-right :right]
                                                                   [second-left :left]
                                                                   [line-reversed-data]
                                                                   (:fimbriation opposite-line)
                                                                   render-options)]
    [:<>
     fimbriation-elements-top
     fimbriation-elements-bottom
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left line-one-start)
                      (svg/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines-top
         fimbriation-outlines-bottom])
      environment ordinary context]]))

(defn chief
  {:display-name "Chief"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                     (options/sanitize ordinary (options ordinary))
        {:keys [size]}                              geometry
        points                                      (:points environment)
        top                                         (:top points)
        top-left                                    (:top-left points)
        left                                        (:left points)
        right                                       (:right points)
        height                                      (:height environment)
        band-height                                 (-> height
                                                        (* size)
                                                        (/ 100))
        row                                         (+ (:y top) band-height)
        row-left                                    (v/v (:x left) row)
        row-right                                   (v/v (:x right) row)
        line                                        (-> line
                                                        (update-in [:fimbriation :thickness-1] (percent-of height))
                                                        (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}    (line/create line
                                                                 (:x (v/- right left))
                                                                 :reversed? true
                                                                 :angle 180
                                                                 :render-options render-options)
        parts                                       [[["M" (v/+ row-right
                                                                line-reversed-start)
                                                       (svg/stitch line-reversed)
                                                       (infinity/path :clockwise
                                                                      [:left :right]
                                                                      [(v/+ row-left
                                                                            line-reversed-start)
                                                                       (v/+ row-right
                                                                            line-reversed-start)])
                                                       "z"]
                                                      [top-left (v/+ row-right
                                                                     line-reversed-start)]]]
        field                                       (if (charge/counterchangable? field parent)
                                                      (charge/counterchange-field field parent)
                                                      field)
        [fimbriation-elements fimbriation-outlines] (fimbriation/render
                                                     [row-right :right]
                                                     [row-left :left]
                                                     [line-reversed-data]
                                                     (:fimbriation line)
                                                     render-options)]
    [:<>
     fimbriation-elements
     [division-shared/make-division
      :ordinary-chief [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ row-right
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines])
      environment ordinary context]]))

(defn base
  {:display-name "Base"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                     (options/sanitize ordinary (options ordinary))
        {:keys [size]}                              geometry
        points                                      (:points environment)
        bottom                                      (:bottom points)
        bottom-right                                (:bottom-right points)
        left                                        (:left points)
        right                                       (:right points)
        height                                      (:height environment)
        band-height                                 (-> height
                                                        (* size)
                                                        (/ 100))
        row                                         (- (:y bottom) band-height)
        row-left                                    (v/v (:x left) row)
        row-right                                   (v/v  (:x right) row)
        line                                        (-> line
                                                        (update-in [:fimbriation :thickness-1] (percent-of height))
                                                        (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}              (line/create line
                                                                 (:x (v/- right left))
                                                                 :render-options render-options)
        parts                                       [[["M" (v/+ row-left
                                                                line-one-start)
                                                       (svg/stitch line-one)
                                                       (infinity/path :clockwise
                                                                      [:right :left]
                                                                      [(v/+ row-right
                                                                            line-one-start)
                                                                       (v/+ row-left
                                                                            line-one-start)])
                                                       "z"]
                                                      [(v/+ row-left
                                                            line-one-start) bottom-right]]]
        field                                       (if (charge/counterchangable? field parent)
                                                      (charge/counterchange-field field parent)
                                                      field)
        [fimbriation-elements fimbriation-outlines] (fimbriation/render
                                                     [row-left :left]
                                                     [row-right :right]
                                                     [line-one-data]
                                                     (:fimbriation line)
                                                     render-options)]
    [:<>
     fimbriation-elements
     [division-shared/make-division
      :ordinary-base [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ row-left
                               line-one-start)
                      (svg/stitch line-one)])}]
         fimbriation-outlines])
      environment ordinary context]]))

(defn bend
  {:display-name "Bend"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}              (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                            geometry
        opposite-line                                             (sanitize-opposite-line ordinary line)
        points                                                    (:points environment)
        top-left                                                  (:top-left points)
        origin-point                                              (position/calculate origin environment :fess)
        left                                                      (:left points)
        right                                                     (:right points)
        height                                                    (:height environment)
        band-height                                               (-> height
                                                                      (* size)
                                                                      (/ 100))
        direction                                                 (angle/direction diagonal-mode points origin-point)
        direction-orthogonal                                      (v/v (-> direction :y) (-> direction :x -))
        diagonal-start                                            (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end                                              (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle                                                     (angle/angle-to-point diagonal-start diagonal-end)
        required-half-length                                      (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        bend-length                                               (* required-half-length 2)
        line-length                                               (-> diagonal-end
                                                                      (v/- diagonal-start)
                                                                      v/abs
                                                                      (* 4))
        period                                                    (-> line
                                                                      :width
                                                                      (or 1))
        offset                                                    (-> period
                                                                      (* (-> line-length
                                                                             (/ 4)
                                                                             (/ period)
                                                                             Math/ceil
                                                                             inc))
                                                                      -)
        row1                                                      (- (/ band-height 2))
        row2                                                      (+ row1 band-height)
        first-left                                                (v/v offset row1)
        first-right                                               (v/v (+ offset line-length) row1)
        second-left                                               (v/v offset row2)
        second-right                                              (v/v (+ offset line-length) row2)
        line                                                      (-> line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                             (-> opposite-line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}                            (line/create line
                                                                               line-length
                                                                               :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}                  (line/create opposite-line
                                                                               line-length
                                                                               :reversed? true
                                                                               :angle 180
                                                                               :render-options render-options)
        parts                                                     [[["M" (v/+ first-left
                                                                              line-one-start)
                                                                     (svg/stitch line-one)
                                                                     (infinity/path :clockwise
                                                                                    [:right :right]
                                                                                    [(v/+ first-right
                                                                                          line-one-start)
                                                                                     (v/+ second-right
                                                                                          line-reversed-start)])
                                                                     (svg/stitch line-reversed)
                                                                     (infinity/path :clockwise
                                                                                    [:left :left]
                                                                                    [(v/+ second-left
                                                                                          line-reversed-start)
                                                                                     (v/+ first-left
                                                                                          line-one-start)])
                                                                     "z"]
                                                                    [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged?                                           (charge/counterchangable? field parent)
        field                                                     (if counterchanged?
                                                                    (charge/counterchange-field field parent)
                                                                    field)
        [fimbriation-elements-top fimbriation-outlines-top]       (fimbriation/render
                                                                   [first-left :left]
                                                                   [first-right :right]
                                                                   [line-one-data]
                                                                   (:fimbriation line)
                                                                   render-options)
        [fimbriation-elements-bottom fimbriation-outlines-bottom] (fimbriation/render
                                                                   [second-right :right]
                                                                   [second-left :left]
                                                                   [line-reversed-data]
                                                                   (:fimbriation opposite-line)
                                                                   render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
     fimbriation-elements-top
     fimbriation-elements-bottom
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left
                               line-one-start)
                      (svg/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines-top
         fimbriation-outlines-bottom])
      environment ordinary (-> context
                               (assoc :transform (when (or counterchanged?
                                                           (:inherit-environment? field))
                                                   (str
                                                    "rotate(" (- angle) ") "
                                                    "translate(" (-> diagonal-start :x -) "," (-> diagonal-start :y -) ")"))))]]))

(defn bend-sinister
  {:display-name "Bend sinister"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}              (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                            geometry
        opposite-line                                             (sanitize-opposite-line ordinary line)
        points                                                    (:points environment)
        top-right                                                 (:top-right points)
        origin-point                                              (position/calculate origin environment :fess)
        left                                                      (:left points)
        right                                                     (:right points)
        height                                                    (:height environment)
        band-height                                               (-> height
                                                                      (* size)
                                                                      (/ 100))
        direction                                                 (angle/direction diagonal-mode points origin-point)
        direction-orthogonal                                      (v/v (-> direction :y) (-> direction :x))
        diagonal-start                                            (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end                                              (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle                                                     (angle/angle-to-point diagonal-start diagonal-end)
        required-half-length                                      (v/distance-point-to-line top-right origin-point (v/+ origin-point direction-orthogonal))
        bend-length                                               (* required-half-length 2)
        line-length                                               (-> diagonal-end
                                                                      (v/- diagonal-start)
                                                                      v/abs
                                                                      (* 4))
        row1                                                      (- (/ band-height 2))
        row2                                                      (+ row1 band-height)
        period                                                    (-> line
                                                                      :width
                                                                      (or 1))
        offset                                                    (-> period
                                                                      (* (-> line-length
                                                                             (/ 4)
                                                                             (/ period)
                                                                             Math/ceil
                                                                             inc))
                                                                      -)
        first-left                                                (v/v offset row1)
        first-right                                               (v/v (+ offset line-length) row1)
        second-left                                               (v/v offset row2)
        second-right                                              (v/v (+ offset line-length) row2)
        line                                                      (-> line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                             (-> opposite-line
                                                                      (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                      (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one       :line
         line-one-start :line-start
         :as            line-one-data}                            (line/create line
                                                                               line-length
                                                                               :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start
         :as                 line-reversed-data}                  (line/create opposite-line
                                                                               line-length
                                                                               :reversed? true
                                                                               :angle 180
                                                                               :render-options render-options)
        parts                                                     [[["M" (v/+ first-left
                                                                              line-one-start)
                                                                     (svg/stitch line-one)
                                                                     (infinity/path :clockwise
                                                                                    [:right :right]
                                                                                    [(v/+ first-right
                                                                                          line-one-start)
                                                                                     (v/+ second-right
                                                                                          line-reversed-start)])
                                                                     (svg/stitch line-reversed)
                                                                     (infinity/path :clockwise
                                                                                    [:left :left]
                                                                                    [(v/+ second-left
                                                                                          line-reversed-start)
                                                                                     (v/+ first-left
                                                                                          line-one-start)])
                                                                     "z"]
                                                                    [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged?                                           (charge/counterchangable? field parent)
        field                                                     (if counterchanged?
                                                                    (charge/counterchange-field field parent)
                                                                    field)
        [fimbriation-elements-top fimbriation-outlines-top]       (fimbriation/render
                                                                   [first-left :left]
                                                                   [first-right :right]
                                                                   [line-one-data]
                                                                   (:fimbriation line)
                                                                   render-options)
        [fimbriation-elements-bottom fimbriation-outlines-bottom] (fimbriation/render
                                                                   [second-right :right]
                                                                   [second-left :left]
                                                                   [line-reversed-data]
                                                                   (:fimbriation opposite-line)
                                                                   render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
     fimbriation-elements-top
     fimbriation-elements-bottom
     [division-shared/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left
                               line-one-start)
                      (svg/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right
                               line-reversed-start)
                      (svg/stitch line-reversed)])}]
         fimbriation-outlines-top
         fimbriation-outlines-bottom])
      environment ordinary (-> context
                               (assoc :transform (when (or counterchanged?
                                                           (:inherit-environment? field))
                                                   (str
                                                    "rotate(" (- angle) ") "
                                                    "translate(" (-> diagonal-start :x -) "," (-> diagonal-start :y -) ")"))))]]))

(defn cross
  {:display-name "Cross"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                             (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                             geometry
        points                                                     (:points environment)
        origin-point                                               (position/calculate origin environment :fess)
        top                                                        (assoc (:top points) :x (:x origin-point))
        bottom                                                     (assoc (:bottom points) :x (:x origin-point))
        left                                                       (assoc (:left points) :y (:y origin-point))
        right                                                      (assoc (:right points) :y (:y origin-point))
        width                                                      (:width environment)
        height                                                     (:height environment)
        band-width                                                 (-> width
                                                                       (* size)
                                                                       (/ 100))
        col1                                                       (- (:x origin-point) (/ band-width 2))
        col2                                                       (+ col1 band-width)
        pale-top-left                                              (v/v col1 (:y top))
        pale-bottom-left                                           (v/v col1 (:y bottom))
        pale-top-right                                             (v/v col2 (:y top))
        pale-bottom-right                                          (v/v col2 (:y bottom))
        row1                                                       (- (:y origin-point) (/ band-width 2))
        row2                                                       (+ row1 band-width)
        fess-top-left                                              (v/v (:x left) row1)
        fess-top-right                                             (v/v (:x right) row1)
        fess-bottom-left                                           (v/v (:x left) row2)
        fess-bottom-right                                          (v/v (:x right) row2)
        corner-top-left                                            (v/v col1 row1)
        corner-top-right                                           (v/v col2 row1)
        corner-bottom-left                                         (v/v col1 row2)
        corner-bottom-right                                        (v/v col2 row2)
        line                                                       (-> line
                                                                       (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                       (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-pale-top-left       :line
         line-pale-top-left-start :line-start
         :as                      line-pale-top-left-data}         (line/create line
                                                                                (v/abs (v/- corner-top-left pale-top-left))
                                                                                :angle -90
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-pale-top-right       :line
         line-pale-top-right-start :line-start
         :as                       line-pale-top-right-data}       (line/create line
                                                                                (v/abs (v/- corner-top-right pale-top-right))
                                                                                :angle 90
                                                                                :reversed? true
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-fess-top-right       :line
         line-fess-top-right-start :line-start
         :as                       line-fess-top-right-data}       (line/create line
                                                                                (v/abs (v/- corner-top-right fess-top-right))
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-fess-bottom-right       :line
         line-fess-bottom-right-start :line-start
         :as                          line-fess-bottom-right-data} (line/create line
                                                                                (v/abs (v/- corner-bottom-right fess-bottom-right))
                                                                                :angle 180
                                                                                :reversed? true
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-pale-bottom-right       :line
         line-pale-bottom-right-start :line-start
         :as                          line-pale-bottom-right-data} (line/create line
                                                                                (v/abs (v/- corner-bottom-right pale-bottom-right))
                                                                                :angle 90
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-pale-bottom-left       :line
         line-pale-bottom-left-start :line-start
         :as                         line-pale-bottom-left-data}   (line/create line
                                                                                (v/abs (v/- corner-bottom-left pale-bottom-left))
                                                                                :angle -90
                                                                                :reversed? true
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-fess-bottom-left       :line
         line-fess-bottom-left-start :line-start
         :as                         line-fess-bottom-left-data}   (line/create line
                                                                                (v/abs (v/- corner-bottom-left fess-bottom-left))
                                                                                :angle 180
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        {line-fess-top-left       :line
         line-fess-top-left-start :line-start
         :as                      line-fess-top-left-data}         (line/create line
                                                                                (v/abs (v/- corner-top-left fess-top-left))
                                                                                :reversed? true
                                                                                :joint-angle 90
                                                                                :render-options render-options)
        parts                                                      [[["M" (v/+ corner-top-left
                                                                               line-pale-top-left-start)
                                                                      (svg/stitch line-pale-top-left)
                                                                      (infinity/path :clockwise
                                                                                     [:top :top]
                                                                                     [(v/+ pale-top-left
                                                                                           line-pale-top-left-start)
                                                                                      (v/+ pale-top-right
                                                                                           line-pale-top-right-start)])
                                                                      (svg/stitch line-pale-top-right)
                                                                      "L" (v/+ corner-top-right
                                                                               line-fess-top-right-start)
                                                                      (svg/stitch line-fess-top-right)
                                                                      (infinity/path :clockwise
                                                                                     [:right :right]
                                                                                     [(v/+ fess-top-right
                                                                                           line-fess-top-right-start)
                                                                                      (v/+ fess-bottom-right
                                                                                           line-fess-bottom-right-start)])
                                                                      (svg/stitch line-fess-bottom-right)
                                                                      "L" (v/+ corner-bottom-right
                                                                               line-pale-bottom-right-start)
                                                                      (svg/stitch line-pale-bottom-right)
                                                                      (infinity/path :clockwise
                                                                                     [:bottom :bottom]
                                                                                     [(v/+ pale-bottom-right
                                                                                           line-pale-bottom-right-start)
                                                                                      (v/+ pale-bottom-left
                                                                                           line-pale-bottom-left-start)])
                                                                      (svg/stitch line-pale-bottom-left)
                                                                      "L" (v/+ corner-bottom-left
                                                                               line-fess-bottom-left-start)
                                                                      (svg/stitch line-fess-bottom-left)
                                                                      (infinity/path :clockwise
                                                                                     [:left :left]
                                                                                     [(v/+ fess-bottom-left
                                                                                           line-fess-bottom-left-start)
                                                                                      (v/+ fess-top-left
                                                                                           line-fess-top-left-start)])
                                                                      (svg/stitch line-fess-top-left)
                                                                      "z"]
                                                                     [top bottom left right]]]
        field                                                      (if (charge/counterchangable? field parent)
                                                                     (charge/counterchange-field field parent)
                                                                     field)
        [fimbriation-elements-1 fimbriation-outlines-1]            (fimbriation/render
                                                                    [fess-top-left :left]
                                                                    [pale-top-left :top]
                                                                    [line-fess-top-left-data
                                                                     line-pale-top-left-data]
                                                                    (:fimbriation line)
                                                                    render-options)
        [fimbriation-elements-2 fimbriation-outlines-2]            (fimbriation/render
                                                                    [pale-top-right :top]
                                                                    [fess-top-right :right]
                                                                    [line-pale-top-right-data
                                                                     line-fess-top-right-data]
                                                                    (:fimbriation line)
                                                                    render-options)
        [fimbriation-elements-3 fimbriation-outlines-3]            (fimbriation/render
                                                                    [fess-bottom-right :right]
                                                                    [pale-bottom-right :bottom]
                                                                    [line-fess-bottom-right-data
                                                                     line-pale-bottom-right-data]
                                                                    (:fimbriation line)
                                                                    render-options)
        [fimbriation-elements-4 fimbriation-outlines-4]            (fimbriation/render
                                                                    [pale-bottom-left :bottom]
                                                                    [fess-bottom-left :left]
                                                                    [line-pale-bottom-left-data
                                                                     line-fess-bottom-left-data]
                                                                    (:fimbriation line)
                                                                    render-options)]
    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     fimbriation-elements-3
     fimbriation-elements-4
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top-left
                               line-pale-top-left-start)
                      (svg/stitch line-pale-top-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-top-right
                               line-pale-top-right-start)
                      (svg/stitch line-pale-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top-right
                               line-fess-top-right-start)
                      (svg/stitch line-fess-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-bottom-right
                               line-fess-bottom-right-start)
                      (svg/stitch line-fess-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-right
                               line-pale-bottom-right-start)
                      (svg/stitch line-pale-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-bottom-left
                               line-pale-bottom-left-start)
                      (svg/stitch line-pale-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-left
                               line-fess-bottom-left-start)
                      (svg/stitch line-fess-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-top-left
                               line-fess-top-left-start)
                      (svg/stitch line-fess-top-left)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4])
      environment ordinary context]]))

(defn saltire
  {:display-name "Saltire"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}                 (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                               geometry
        points                                                       (:points environment)
        origin-point                                                 (position/calculate origin environment :fess)
        top                                                          (assoc (:top points) :x (:x origin-point))
        bottom                                                       (assoc (:bottom points) :x (:x origin-point))
        left                                                         (assoc (:left points) :y (:y origin-point))
        right                                                        (assoc (:right points) :y (:y origin-point))
        width                                                        (:width environment)
        height                                                       (:height environment)
        band-width                                                   (-> width
                                                                         (* size)
                                                                         (/ 100))
        direction                                                    (angle/direction diagonal-mode points origin-point)
        arm-length                                                   (-> direction
                                                                         v/abs
                                                                         (->> (/ height)))
        diagonal-top-left                                            (v/+ origin-point (v/* (v/dot direction (v/v -1 -1)) arm-length))
        diagonal-top-right                                           (v/+ origin-point (v/* (v/dot direction (v/v 1 -1)) arm-length))
        diagonal-bottom-left                                         (v/+ origin-point (v/* (v/dot direction (v/v -1 1)) arm-length))
        diagonal-bottom-right                                        (v/+ origin-point (v/* (v/dot direction (v/v 1 1)) arm-length))
        angle-top-left                                               (angle/angle-to-point origin-point diagonal-top-left)
        angle-top-right                                              (angle/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                                            (angle/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                                           (angle/angle-to-point origin-point diagonal-bottom-right)
        angle                                                        (-> angle-bottom-right (* Math/PI) (/ 180))
        joint-angle-horizontal                                       (+ 360 (- angle-top-left angle-bottom-left))
        joint-angle-vertical                                         (- angle-top-right angle-top-left)
        dx                                                           (/ band-width 2 (Math/sin angle))
        dy                                                           (/ band-width 2 (Math/cos angle))
        offset-top                                                   (v/v 0 (- dy))
        offset-bottom                                                (v/v 0 dy)
        offset-left                                                  (v/v (- dx) 0)
        offset-right                                                 (v/v dx 0)
        corner-top                                                   (v/+ origin-point offset-top)
        corner-bottom                                                (v/+ origin-point offset-bottom)
        corner-left                                                  (v/+ origin-point offset-left)
        corner-right                                                 (v/+ origin-point offset-right)
        top-left-upper                                               (v/+ diagonal-top-left offset-top)
        top-left-lower                                               (v/+ diagonal-top-left offset-bottom)
        top-right-upper                                              (v/+ diagonal-top-right offset-top)
        top-right-lower                                              (v/+ diagonal-top-right offset-bottom)
        bottom-left-upper                                            (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                                            (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                                           (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                                           (v/+ diagonal-bottom-right offset-bottom)
        line                                                         (-> line
                                                                         (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                         (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-top-left-lower       :line
         line-top-left-lower-start :line-start
         :as                       line-top-left-lower-data}         (line/create line
                                                                                  (v/abs (v/- corner-left top-left-lower))
                                                                                  :angle  angle-top-left
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options   render-options)
        {line-top-left-upper       :line
         line-top-left-upper-start :line-start
         :as                       line-top-left-upper-data}         (line/create line
                                                                                  (v/abs (v/- corner-top top-left-upper))
                                                                                  :angle     (- angle-top-left 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-top-right-upper       :line
         line-top-right-upper-start :line-start
         :as                        line-top-right-upper-data}       (line/create line
                                                                                  (v/abs (v/- corner-top top-right-upper))
                                                                                  :angle angle-top-right
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-top-right-lower       :line
         line-top-right-lower-start :line-start
         :as                        line-top-right-lower-data}       (line/create line
                                                                                  (v/abs (v/- corner-right top-right-lower))
                                                                                  :angle (- angle-top-right 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        {line-bottom-right-upper       :line
         line-bottom-right-upper-start :line-start
         :as                           line-bottom-right-upper-data} (line/create line
                                                                                  (v/abs (v/- corner-right bottom-right-upper))
                                                                                  :angle angle-bottom-right
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        {line-bottom-right-lower       :line
         line-bottom-right-lower-start :line-start
         :as                           line-bottom-right-lower-data} (line/create line
                                                                                  (v/abs (v/- corner-bottom bottom-right-lower))
                                                                                  :angle (- angle-bottom-right 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-bottom-left-lower       :line
         line-bottom-left-lower-start :line-start
         :as                          line-bottom-left-lower-data}   (line/create line
                                                                                  (v/abs (v/- corner-bottom bottom-left-lower))
                                                                                  :angle angle-bottom-left
                                                                                  :joint-angle joint-angle-vertical
                                                                                  :render-options render-options)
        {line-bottom-left-upper       :line
         line-bottom-left-upper-start :line-start
         :as                          line-bottom-left-upper-data}   (line/create line
                                                                                  (v/abs (v/- corner-left bottom-left-upper))
                                                                                  :angle (- angle-bottom-left 180)
                                                                                  :reversed? true
                                                                                  :joint-angle joint-angle-horizontal
                                                                                  :render-options render-options)
        parts                                                        [[["M" (v/+ corner-left
                                                                                 line-top-left-lower-start)
                                                                        (svg/stitch line-top-left-lower)
                                                                        (infinity/path :clockwise
                                                                                       [:left :left]
                                                                                       [(v/+ top-left-lower
                                                                                             line-top-left-lower-start)
                                                                                        (v/+ top-left-upper
                                                                                             line-top-left-upper-start)])
                                                                        (svg/stitch line-top-left-upper)
                                                                        "L" (v/+ corner-top
                                                                                 line-top-right-upper-start)
                                                                        (svg/stitch line-top-right-upper)
                                                                        (infinity/path :clockwise
                                                                                       [:right :right]
                                                                                       [(v/+ top-right-upper
                                                                                             line-top-right-upper-start)
                                                                                        (v/+ top-right-lower
                                                                                             line-top-right-lower-start)])
                                                                        (svg/stitch line-top-right-lower)
                                                                        "L" (v/+ corner-right
                                                                                 line-bottom-right-upper-start)
                                                                        (svg/stitch line-bottom-right-upper)
                                                                        (infinity/path :clockwise
                                                                                       [:right :right]
                                                                                       [(v/+ bottom-right-upper
                                                                                             line-bottom-right-upper-start)
                                                                                        (v/+ bottom-right-lower
                                                                                             line-bottom-right-lower-start)])
                                                                        (svg/stitch line-bottom-right-lower)
                                                                        "L" (v/+ corner-bottom
                                                                                 line-bottom-left-lower-start)
                                                                        (svg/stitch line-bottom-left-lower)
                                                                        (infinity/path :clockwise
                                                                                       [:left :left]
                                                                                       [(v/+ bottom-left-lower
                                                                                             line-bottom-left-lower)
                                                                                        (v/+ bottom-left-upper
                                                                                             line-bottom-left-upper-start)])
                                                                        (svg/stitch line-bottom-left-upper)
                                                                        "z"]
                                                                       [top bottom left right]]]
        field                                                        (if (charge/counterchangable? field parent)
                                                                       (charge/counterchange-field field parent)
                                                                       field)
        [fimbriation-elements-1 fimbriation-outlines-1]              (fimbriation/render
                                                                      [top-left-upper :left]
                                                                      [top-right-upper :right]
                                                                      [line-top-left-upper-data
                                                                       line-top-right-upper-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-2 fimbriation-outlines-2]              (fimbriation/render
                                                                      [top-right-lower :right]
                                                                      [bottom-right-upper :right]
                                                                      [line-top-right-lower-data
                                                                       line-bottom-right-upper-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-3 fimbriation-outlines-3]              (fimbriation/render
                                                                      [bottom-right-lower :right]
                                                                      [bottom-left-lower :left]
                                                                      [line-bottom-right-lower-data
                                                                       line-bottom-left-lower-data]
                                                                      (:fimbriation line)
                                                                      render-options)
        [fimbriation-elements-4 fimbriation-outlines-4]              (fimbriation/render
                                                                      [bottom-left-upper :left]
                                                                      [top-left-lower :left]
                                                                      [line-bottom-left-upper-data
                                                                       line-top-left-lower-data]
                                                                      (:fimbriation line)
                                                                      render-options)]

    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     fimbriation-elements-3
     fimbriation-elements-4
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-left
                               line-top-left-lower-start)
                      (svg/stitch line-top-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ top-left-upper
                               line-top-left-upper-start)
                      (svg/stitch line-top-left-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top
                               line-top-right-upper-start)
                      (svg/stitch line-top-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ top-right-lower
                               line-top-right-lower-start)
                      (svg/stitch line-top-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-right
                               line-bottom-right-upper-start)
                      (svg/stitch line-bottom-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-right-lower
                               line-bottom-right-lower-start)
                      (svg/stitch line-bottom-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom
                               line-bottom-left-lower-start)
                      (svg/stitch line-bottom-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-left-upper
                               line-bottom-left-upper-start)
                      (svg/stitch line-bottom-left-upper)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4])
      environment ordinary context]]))

(defn chevron
  {:display-name "Chevron"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}            (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                          geometry
        opposite-line                                           (sanitize-opposite-line ordinary line)
        points                                                  (:points environment)
        origin-point                                            (position/calculate origin environment :fess)
        top                                                     (assoc (:top points) :x (:x origin-point))
        bottom                                                  (assoc (:bottom points) :x (:x origin-point))
        left                                                    (assoc (:left points) :y (:y origin-point))
        right                                                   (assoc (:right points) :y (:y origin-point))
        height                                                  (:height environment)
        band-width                                              (-> height
                                                                    (* size)
                                                                    (/ 100))
        direction                                               (angle/direction diagonal-mode points origin-point)
        diagonal-left                                           (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-right                                          (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-left                                              (angle/angle-to-point origin-point diagonal-left)
        angle-right                                             (angle/angle-to-point origin-point diagonal-right)
        angle                                                   (-> angle-right (* Math/PI) (/ 180))
        joint-angle                                             (- angle-left angle-right)
        dy                                                      (/ band-width 2 (Math/cos angle))
        offset-top                                              (v/v 0 (- dy))
        offset-bottom                                           (v/v 0 dy)
        corner-upper                                            (v/+ origin-point offset-top)
        corner-lower                                            (v/+ origin-point offset-bottom)
        left-upper                                              (v/+ diagonal-left offset-top)
        left-lower                                              (v/+ diagonal-left offset-bottom)
        right-upper                                             (v/+ diagonal-right offset-top)
        right-lower                                             (v/+ diagonal-right offset-bottom)
        line                                                    (-> line
                                                                    (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                    (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                           (-> opposite-line
                                                                    (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                    (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-right-upper       :line
         line-right-upper-start :line-start
         :as                    line-right-upper-data}          (line/create line
                                                                             (v/abs (v/- corner-upper right-upper))
                                                                             :angle angle-right
                                                                             :render-options render-options
                                                                             :joint-angle (- joint-angle))
        {line-right-lower       :line
         line-right-lower-start :line-start
         :as                    line-right-lower-data}          (line/create opposite-line
                                                                             (v/abs (v/- corner-lower right-lower))
                                                                             :angle (- angle-right 180)
                                                                             :reversed? true
                                                                             :render-options render-options
                                                                             :joint-angle joint-angle)
        {line-left-lower       :line
         line-left-lower-start :line-start
         :as                   line-left-lower-data}            (line/create opposite-line
                                                                             (v/abs (v/- corner-lower left-lower))
                                                                             :angle angle-left
                                                                             :render-options render-options
                                                                             :joint-angle joint-angle)
        {line-left-upper       :line
         line-left-upper-start :line-start
         :as                   line-left-upper-data}            (line/create line
                                                                             (v/abs (v/- corner-upper left-upper))
                                                                             :angle (- angle-left 180)
                                                                             :reversed? true
                                                                             :render-options render-options
                                                                             :joint-angle (- joint-angle))
        parts                                                   [[["M" (v/+ corner-upper
                                                                            line-right-upper-start)
                                                                   (svg/stitch line-right-upper)
                                                                   (infinity/path :clockwise
                                                                                  [:right :right]
                                                                                  [(v/+ right-upper
                                                                                        line-right-upper-start)
                                                                                   (v/+ right-lower
                                                                                        line-right-lower-start)])
                                                                   (svg/stitch line-right-lower)
                                                                   "L" (v/+ corner-lower
                                                                            line-left-lower-start)
                                                                   (svg/stitch line-left-lower)
                                                                   (infinity/path :clockwise
                                                                                  [:left :left]
                                                                                  [(v/+ left-lower
                                                                                        line-left-lower-start)
                                                                                   (v/+ left-upper
                                                                                        line-left-upper-start)])
                                                                   (svg/stitch line-left-upper)
                                                                   "z"]
                                                                  [top bottom left right]]]
        field                                                   (if (charge/counterchangable? field parent)
                                                                  (charge/counterchange-field field parent)
                                                                  field)
        [fimbriation-elements-upper fimbriation-outlines-upper] (fimbriation/render
                                                                 [left-upper :left]
                                                                 [right-upper :right]
                                                                 [line-left-upper-data
                                                                  line-right-upper-data]
                                                                 (:fimbriation line)
                                                                 render-options)
        [fimbriation-elements-lower fimbriation-outlines-lower] (fimbriation/render
                                                                 [right-lower :bottom]
                                                                 [left-lower :bottom]
                                                                 [line-right-lower-data
                                                                  line-left-lower-data]
                                                                 (:fimbriation opposite-line)
                                                                 render-options)]
    [:<>
     fimbriation-elements-upper
     fimbriation-elements-lower
     [division-shared/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g outline/style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-upper
                               line-right-upper-start)
                      (svg/stitch line-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ right-lower
                               line-right-lower-start)
                      (svg/stitch line-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-lower
                               line-left-lower-start)
                      (svg/stitch line-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ left-upper
                               line-left-upper-start)
                      (svg/stitch line-left-upper)])}]
         fimbriation-outlines-upper
         fimbriation-outlines-lower])
      environment ordinary context]]))

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

(def ordinary-map
  (util/choices->map choices))

(defn render [{:keys [type] :as ordinary} parent environment context]
  (let [function (get kinds-function-map type)]
    [function ordinary parent environment context]))
