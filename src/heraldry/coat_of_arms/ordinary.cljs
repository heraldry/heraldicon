(ns heraldry.coat-of-arms.ordinary
  (:require [heraldry.coat-of-arms.charge :as charge]
            [heraldry.coat-of-arms.division :as division]
            [heraldry.coat-of-arms.geometry :as geometry]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture :as tincture]
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

(defn render-tinctured-area [tincture path render-options]
  (let [mask-id (util/id "mask")]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d    (svg/make-path path)
               :fill "#ffffff"}]]]
     [:rect {:x      -500
             :y      -500
             :width  1100
             :height 1100
             :mask   (str "url(#" mask-id ")")
             :fill   (tincture/pick tincture render-options)}]]))

(defn percent-of [value]
  (fn [v]
    (when v
      (-> v
          (* 100)
          (/ value)))))

(defn render-fimbriation [start end [first second]
                          {line                      :line
                           line-offset               :start-offset
                           line-fimbriation-1        :fimbriation-1
                           line-fimbriation-1-offset :fimbriation-1-offset
                           line-fimbriation-2        :fimbriation-2
                           line-fimbriation-2-offset :fimbriation-2-offset}
                          {:keys [:tincture-1 :tincture-2]}
                          render-options]
  (let [fimbriation-elements [:<>
                              (when line-fimbriation-2
                                [render-tinctured-area
                                 tincture-2
                                 ["M" (v/+ start line-offset)
                                  (line/stitch line)
                                  (if (= second :none)
                                    ["L" (v/+ end
                                              line-fimbriation-2-offset)]
                                    (infinity/path :counter-clockwise
                                                   [second second]
                                                   [(v/+ end
                                                         line-offset)
                                                    (v/+ end
                                                         line-fimbriation-2-offset)]))
                                  (line/stitch line-fimbriation-2)
                                  (if (= first :none)
                                    ["L" (v/+ start
                                              line-offset)]
                                    (infinity/path :counter-clockwise
                                                   [first first]
                                                   [(v/+ start
                                                         line-fimbriation-2-offset)
                                                    (v/+ start
                                                         line-offset)]))
                                  "z"]
                                 render-options])
                              (when line-fimbriation-1
                                [render-tinctured-area
                                 tincture-1
                                 ["M" (v/+ start line-offset)
                                  (line/stitch line)
                                  (if (= second :none)
                                    ["L" (v/+ end
                                              line-fimbriation-1-offset)]
                                    (infinity/path :counter-clockwise
                                                   [second second]
                                                   [(v/+ end
                                                         line-offset)
                                                    (v/+ end
                                                         line-fimbriation-1-offset)]))
                                  (line/stitch line-fimbriation-1)
                                  (if (= first :none)
                                    ["L" (v/+ start
                                              line-offset)]
                                    (infinity/path :counter-clockwise
                                                   [first first]
                                                   [(v/+ start
                                                         line-fimbriation-1-offset)
                                                    (v/+ start
                                                         line-offset)]))
                                  "z"]
                                 render-options])]
        fimbriation-outlines [:<>
                              (when line-fimbriation-1
                                [:path {:d (svg/make-path
                                            ["M" (v/+ end line-fimbriation-1-offset)
                                             (line/stitch line-fimbriation-1)])}])
                              (when line-fimbriation-2
                                [:path {:d (svg/make-path
                                            ["M" (v/+ end line-fimbriation-2-offset)
                                             (line/stitch line-fimbriation-2)])}])]]
    [fimbriation-elements fimbriation-outlines]))

(defn pale
  {:display-name "Pale"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                  (options/sanitize ordinary (options ordinary))
        opposite-line                                   (sanitize-opposite-line ordinary line)
        {:keys [size]}                                  geometry
        points                                          (:points environment)
        origin-point                                    (position/calculate origin environment :fess)
        top                                             (assoc (:top points) :x (:x origin-point))
        bottom                                          (assoc (:bottom points) :x (:x origin-point))
        width                                           (:width environment)
        band-width                                      (-> width
                                                            (* size)
                                                            (/ 100))
        col1                                            (- (:x origin-point) (/ band-width 2))
        col2                                            (+ col1 band-width)
        first-top                                       (v/v col1 (:y top))
        first-bottom                                    (v/v col1 (:y bottom))
        second-top                                      (v/v col2 (:y top))
        second-bottom                                   (v/v col2 (:y bottom))
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of width))
                                                            (update-in [:fimbriation :thickness-2] (percent-of width)))
        opposite-line                                   (-> opposite-line
                                                            (update-in [:fimbriation :thickness-1] (percent-of width))
                                                            (update-in [:fimbriation :thickness-2] (percent-of width)))
        {line-one        :line
         line-one-offset :start-offset
         :as             line-one-data}                 (line/create line
                                                                     (:y (v/- bottom top))
                                                                     :angle -90
                                                                     :reversed? true
                                                                     :flipped? true
                                                                     :render-options render-options)
        {line-reversed        :line
         line-reversed-offset :start-offset
         :as                  line-reversed-data}       (line/create opposite-line
                                                                     (:y (v/- bottom top))
                                                                     :angle 90
                                                                     :flipped? true
                                                                     :render-options render-options)
        parts                                           [[["M" (v/+ first-bottom
                                                                    line-one-offset)
                                                           (line/stitch line-one)
                                                           (infinity/path :clockwise
                                                                          [:top :top]
                                                                          [(v/+ first-top
                                                                                line-one-offset)
                                                                           (v/+ second-top
                                                                                line-reversed-offset)])
                                                           (line/stitch line-reversed)
                                                           (infinity/path :clockwise
                                                                          [:bottom :bottom]
                                                                          [(v/+ second-bottom
                                                                                line-reversed-offset)
                                                                           (v/+ first-bottom
                                                                                line-one-offset)])
                                                           "z"]
                                                          [(v/+ first-bottom
                                                                line-one-offset)
                                                           (v/+ second-top
                                                                line-reversed-offset)]]]
        field                                           (if (charge/counterchangable? field parent)
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation first-bottom first-top
                                                                            [:bottom :top]
                                                                            line-one-data
                                                                            (:fimbriation line)
                                                                            render-options)
        [fimbriation-elements-2 fimbriation-outlines-2] (render-fimbriation second-top second-bottom
                                                                            [:top :bottom]
                                                                            line-reversed-data
                                                                            (:fimbriation opposite-line)
                                                                            render-options)]
    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     [division/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-bottom
                               line-one-offset)
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-top
                               line-reversed-offset)
                      (line/stitch line-reversed)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2])
      environment ordinary context]]))

(defn fess
  {:display-name "Fess"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                  (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                  geometry
        opposite-line                                   (sanitize-opposite-line ordinary line)
        points                                          (:points environment)
        origin-point                                    (position/calculate origin environment :fess)
        left                                            (assoc (:left points) :y (:y origin-point))
        right                                           (assoc (:right points) :y (:y origin-point))
        height                                          (:height environment)
        band-height                                     (-> height
                                                            (* size)
                                                            (/ 100))
        row1                                            (- (:y origin-point) (/ band-height 2))
        row2                                            (+ row1 band-height)
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                   (-> opposite-line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one        :line
         line-one-offset :start-offset
         :as             line-one-data}                 (line/create line
                                                                     (:x (v/- right left))
                                                                     :render-options render-options)
        {line-reversed        :line
         line-reversed-offset :start-offset
         :as                  line-reversed-data}       (line/create opposite-line
                                                                     (:x (v/- right left))
                                                                     :reversed? true
                                                                     :angle 180
                                                                     :render-options render-options)
        first-left                                      (v/v (:x left) row1)
        first-right                                     (v/v (:x right) row1)
        second-left                                     (v/v (:x left) row2)
        second-right                                    (v/v (:x right) row2)
        parts                                           [[["M" (v/+ first-left
                                                                    line-one-offset)
                                                           (line/stitch line-one)
                                                           (infinity/path :clockwise
                                                                          [:right :right]
                                                                          [(v/+ first-right
                                                                                line-one-offset)
                                                                           (v/+ second-right
                                                                                line-reversed-offset)])
                                                           (line/stitch line-reversed)
                                                           (infinity/path :clockwise
                                                                          [:left :left]
                                                                          [(v/+ second-left
                                                                                line-reversed-offset)
                                                                           (v/+ first-left
                                                                                line-one-offset)])
                                                           "z"]
                                                          [(v/+ first-right
                                                                line-one-offset)
                                                           (v/+ second-left
                                                                line-reversed-offset)]]]
        field                                           (if (charge/counterchangable? field parent)
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation first-left first-right
                                                                            [:left :right]
                                                                            line-one-data
                                                                            (:fimbriation line)
                                                                            render-options)
        [fimbriation-elements-2 fimbriation-outlines-2] (render-fimbriation second-right second-left
                                                                            [:right :left]
                                                                            line-reversed-data
                                                                            (:fimbriation opposite-line)
                                                                            render-options)]
    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left line-one-offset)
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right line-reversed-offset)
                      (line/stitch line-reversed)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2])
      environment ordinary context]]))

(defn chief
  {:display-name "Chief"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                         (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                  geometry
        points                                          (:points environment)
        top                                             (:top points)
        top-left                                        (:top-left points)
        left                                            (:left points)
        right                                           (:right points)
        height                                          (:height environment)
        band-height                                     (-> height
                                                            (* size)
                                                            (/ 100))
        row                                             (+ (:y top) band-height)
        row-left                                        (v/v (:x left) row)
        row-right                                       (v/v (:x right) row)
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-reversed        :line
         line-reversed-offset :start-offset
         :as                  line-reversed-data}       (line/create line
                                                                     (:x (v/- right left))
                                                                     :reversed? true
                                                                     :angle 180
                                                                     :render-options render-options)
        parts                                           [[["M" (v/+ row-right
                                                                    line-reversed-offset)
                                                           (line/stitch line-reversed)
                                                           (infinity/path :clockwise
                                                                          [:left :right]
                                                                          [(v/+ row-left
                                                                                line-reversed-offset)
                                                                           (v/+ row-right
                                                                                line-reversed-offset)])
                                                           "z"]
                                                          [top-left (v/+ row-right
                                                                         line-reversed-offset)]]]
        field                                           (if (charge/counterchangable? field parent)
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation row-right row-left
                                                                            [:right :left]
                                                                            line-reversed-data
                                                                            (:fimbriation line)
                                                                            render-options)]
    [:<>
     fimbriation-elements-1
     [division/make-division
      :ordinary-chief [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ row-right
                               line-reversed-offset)
                      (line/stitch line-reversed)])}]
         fimbriation-outlines-1])
      environment ordinary context]]))

(defn base
  {:display-name "Base"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line geometry]}                         (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                  geometry
        points                                          (:points environment)
        bottom                                          (:bottom points)
        bottom-right                                    (:bottom-right points)
        left                                            (:left points)
        right                                           (:right points)
        height                                          (:height environment)
        band-height                                     (-> height
                                                            (* size)
                                                            (/ 100))
        row                                             (- (:y bottom) band-height)
        row-left                                        (v/v (:x left) row)
        row-right                                       (v/v  (:x right) row)
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one        :line
         line-one-offset :start-offset
         :as             line-one-data}                 (line/create line
                                                                     (:x (v/- right left))
                                                                     :render-options render-options)
        parts                                           [[["M" (v/+ row-left
                                                                    line-one-offset)
                                                           (line/stitch line-one)
                                                           (infinity/path :clockwise
                                                                          [:right :left]
                                                                          [(v/+ row-right
                                                                                line-one-offset)
                                                                           (v/+ row-left
                                                                                line-one-offset)])
                                                           "z"]
                                                          [(v/+ row-left
                                                                line-one-offset) bottom-right]]]
        field                                           (if (charge/counterchangable? field parent)
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation row-left row-right
                                                                            [:left :right]
                                                                            line-one-data
                                                                            (:fimbriation line)
                                                                            render-options)]
    [:<>
     fimbriation-elements-1
     [division/make-division
      :ordinary-base [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ row-left
                               line-one-offset)
                      (line/stitch line-one)])}]
         fimbriation-outlines-1])
      environment ordinary context]]))

(defn bend
  {:display-name "Bend"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}    (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                  geometry
        opposite-line                                   (sanitize-opposite-line ordinary line)
        points                                          (:points environment)
        top-left                                        (:top-left points)
        origin-point                                    (position/calculate origin environment :fess)
        left                                            (:left points)
        right                                           (:right points)
        height                                          (:height environment)
        band-height                                     (-> height
                                                            (* size)
                                                            (/ 100))
        direction                                       (division/direction diagonal-mode points origin-point)
        direction-orthogonal                            (v/v (-> direction :y) (-> direction :x -))
        diagonal-start                                  (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-end                                    (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle                                           (division/angle-to-point diagonal-start diagonal-end)
        required-half-length                            (v/distance-point-to-line top-left origin-point (v/+ origin-point direction-orthogonal))
        bend-length                                     (* required-half-length 2)
        line-length                                     (-> diagonal-end
                                                            (v/- diagonal-start)
                                                            v/abs
                                                            (* 4))
        period                                          (-> line
                                                            :width
                                                            (or 1))
        offset                                          (-> period
                                                            (* (-> line-length
                                                                   (/ 4)
                                                                   (/ period)
                                                                   Math/ceil
                                                                   inc))
                                                            -)
        row1                                            (- (/ band-height 2))
        row2                                            (+ row1 band-height)
        first-left                                      (v/v offset row1)
        first-right                                     (v/v (+ offset line-length) row1)
        second-left                                     (v/v offset row2)
        second-right                                    (v/v (+ offset line-length) row2)
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                   (-> opposite-line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one        :line
         line-one-offset :start-offset
         :as             line-one-data}                 (line/create line
                                                                     line-length
                                                                     :render-options render-options)
        {line-reversed        :line
         line-reversed-offset :start-offset
         :as                  line-reversed-data}       (line/create opposite-line
                                                                     line-length
                                                                     :reversed? true
                                                                     :angle 180
                                                                     :render-options render-options)
        parts                                           [[["M" (v/+ first-left
                                                                    line-one-offset)
                                                           (line/stitch line-one)
                                                           (infinity/path :clockwise
                                                                          [:right :right]
                                                                          [(v/+ first-right
                                                                                line-one-offset)
                                                                           (v/+ second-right
                                                                                line-reversed-offset)])
                                                           (line/stitch line-reversed)
                                                           (infinity/path :clockwise
                                                                          [:left :left]
                                                                          [(v/+ second-left
                                                                                line-reversed-offset)
                                                                           (v/+ first-left
                                                                                line-one-offset)])
                                                           "z"]
                                                          [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged?                                 (charge/counterchangable? field parent)
        field                                           (if counterchanged?
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation first-left first-right
                                                                            [:left :right]
                                                                            line-one-data
                                                                            (:fimbriation line)
                                                                            render-options)
        [fimbriation-elements-2 fimbriation-outlines-2] (render-fimbriation second-right second-left
                                                                            [:right :left]
                                                                            line-reversed-data
                                                                            (:fimbriation opposite-line)
                                                                            render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
     fimbriation-elements-1
     fimbriation-elements-2
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left
                               line-one-offset)
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right
                               line-reversed-offset)
                      (line/stitch line-reversed)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2])
      environment ordinary (-> context
                               (assoc :transform (when (or counterchanged?
                                                           (:inherit-environment? field))
                                                   (str
                                                    "rotate(" (- angle) ") "
                                                    "translate(" (-> diagonal-start :x -) "," (-> diagonal-start :y -) ")"))))]]))

(defn bend-sinister
  {:display-name "Bend sinister"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}    (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                  geometry
        opposite-line                                   (sanitize-opposite-line ordinary line)
        points                                          (:points environment)
        top-right                                       (:top-right points)
        origin-point                                    (position/calculate origin environment :fess)
        left                                            (:left points)
        right                                           (:right points)
        height                                          (:height environment)
        band-height                                     (-> height
                                                            (* size)
                                                            (/ 100))
        direction                                       (division/direction diagonal-mode points origin-point)
        direction-orthogonal                            (v/v (-> direction :y) (-> direction :x))
        diagonal-start                                  (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-end                                    (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        angle                                           (division/angle-to-point diagonal-start diagonal-end)
        required-half-length                            (v/distance-point-to-line top-right origin-point (v/+ origin-point direction-orthogonal))
        bend-length                                     (* required-half-length 2)
        line-length                                     (-> diagonal-end
                                                            (v/- diagonal-start)
                                                            v/abs
                                                            (* 4))
        row1                                            (- (/ band-height 2))
        row2                                            (+ row1 band-height)
        period                                          (-> line
                                                            :width
                                                            (or 1))
        offset                                          (-> period
                                                            (* (-> line-length
                                                                   (/ 4)
                                                                   (/ period)
                                                                   Math/ceil
                                                                   inc))
                                                            -)
        first-left                                      (v/v offset row1)
        first-right                                     (v/v (+ offset line-length) row1)
        second-left                                     (v/v offset row2)
        second-right                                    (v/v (+ offset line-length) row2)
        line                                            (-> line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                   (-> opposite-line
                                                            (update-in [:fimbriation :thickness-1] (percent-of height))
                                                            (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-one        :line
         line-one-offset :start-offset
         :as             line-one-data}                 (line/create line
                                                                     line-length
                                                                     :render-options render-options)
        {line-reversed        :line
         line-reversed-offset :start-offset
         :as                  line-reversed-data}       (line/create opposite-line
                                                                     line-length
                                                                     :reversed? true
                                                                     :angle 180
                                                                     :render-options render-options)
        parts                                           [[["M" (v/+ first-left
                                                                    line-one-offset)
                                                           (line/stitch line-one)
                                                           (infinity/path :clockwise
                                                                          [:right :right]
                                                                          [(v/+ first-right
                                                                                line-one-offset)
                                                                           (v/+ second-right
                                                                                line-reversed-offset)])
                                                           (line/stitch line-reversed)
                                                           (infinity/path :clockwise
                                                                          [:left :left]
                                                                          [(v/+ second-left
                                                                                line-reversed-offset)
                                                                           (v/+ first-left
                                                                                line-one-offset)])
                                                           "z"]
                                                          [(v/v 0 row1) (v/v bend-length row2)]]]
        counterchanged?                                 (charge/counterchangable? field parent)
        field                                           (if counterchanged?
                                                          (charge/counterchange-field field parent)
                                                          field)
        [fimbriation-elements-1 fimbriation-outlines-1] (render-fimbriation first-left first-right
                                                                            [:left :right]
                                                                            line-one-data
                                                                            (:fimbriation line)
                                                                            render-options)
        [fimbriation-elements-2 fimbriation-outlines-2] (render-fimbriation second-right second-left
                                                                            [:right :left]
                                                                            line-reversed-data
                                                                            (:fimbriation opposite-line)
                                                                            render-options)]
    [:g {:transform (str "translate(" (:x origin-point) "," (:y origin-point) ")"
                         "rotate(" angle ")"
                         "translate(" (- required-half-length) "," 0 ")")}
     fimbriation-elements-1
     fimbriation-elements-2
     [division/make-division
      :ordinary-fess [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ first-left
                               line-one-offset)
                      (line/stitch line-one)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ second-right
                               line-reversed-offset)
                      (line/stitch line-reversed)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2])
      environment ordinary (-> context
                               (assoc :transform (when (or counterchanged?
                                                           (:inherit-environment? field))
                                                   (str
                                                    "rotate(" (- angle) ") "
                                                    "translate(" (-> diagonal-start :x -) "," (-> diagonal-start :y -) ")"))))]]))

(defn cross
  {:display-name "Cross"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin geometry]}                              (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                              geometry
        points                                                      (:points environment)
        origin-point                                                (position/calculate origin environment :fess)
        top                                                         (assoc (:top points) :x (:x origin-point))
        bottom                                                      (assoc (:bottom points) :x (:x origin-point))
        left                                                        (assoc (:left points) :y (:y origin-point))
        right                                                       (assoc (:right points) :y (:y origin-point))
        width                                                       (:width environment)
        band-width                                                  (-> width
                                                                        (* size)
                                                                        (/ 100))
        col1                                                        (- (:x origin-point) (/ band-width 2))
        col2                                                        (+ col1 band-width)
        pale-top-left                                               (v/v col1 (:y top))
        pale-bottom-left                                            (v/v col1 (:y bottom))
        pale-top-right                                              (v/v col2 (:y top))
        pale-bottom-right                                           (v/v col2 (:y bottom))
        row1                                                        (- (:y origin-point) (/ band-width 2))
        row2                                                        (+ row1 band-width)
        fess-top-left                                               (v/v (:x left) row1)
        fess-top-right                                              (v/v (:x right) row1)
        fess-bottom-left                                            (v/v (:x left) row2)
        fess-bottom-right                                           (v/v (:x right) row2)
        corner-top-left                                             (v/v col1 row1)
        corner-top-right                                            (v/v col2 row1)
        corner-bottom-left                                          (v/v col1 row2)
        corner-bottom-right                                         (v/v col2 row2)
        line                                                        (-> line
                                                                        (update-in [:fimbriation :thickness-1] (percent-of width))
                                                                        (update-in [:fimbriation :thickness-2] (percent-of width)))
        {line-pale-top-left        :line
         line-pale-top-left-offset :start-offset
         :as                       line-pale-top-left-data}         (line/create line
                                                                                 (v/abs (v/- corner-top-left pale-top-left))
                                                                                 :angle -90
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-pale-top-right        :line
         line-pale-top-right-offset :start-offset
         :as                        line-pale-top-right-data}       (line/create line
                                                                                 (v/abs (v/- corner-top-right pale-top-right))
                                                                                 :angle 90
                                                                                 :reversed? true
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-fess-top-right        :line
         line-fess-top-right-offset :start-offset
         :as                        line-fess-top-right-data}       (line/create line
                                                                                 (v/abs (v/- corner-top-right fess-top-right))
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-fess-bottom-right        :line
         line-fess-bottom-right-offset :start-offset
         :as                           line-fess-bottom-right-data} (line/create line
                                                                                 (v/abs (v/- corner-bottom-right fess-bottom-right))
                                                                                 :angle 180
                                                                                 :reversed? true
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-pale-bottom-right        :line
         line-pale-bottom-right-offset :start-offset
         :as                           line-pale-bottom-right-data} (line/create line
                                                                                 (v/abs (v/- corner-bottom-right pale-bottom-right))
                                                                                 :angle 90
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-pale-bottom-left        :line
         line-pale-bottom-left-offset :start-offset
         :as                          line-pale-bottom-left-data}   (line/create line
                                                                                 (v/abs (v/- corner-bottom-left pale-bottom-left))
                                                                                 :angle -90
                                                                                 :reversed? true
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-fess-bottom-left        :line
         line-fess-bottom-left-offset :start-offset
         :as                          line-fess-bottom-left-data}   (line/create line
                                                                                 (v/abs (v/- corner-bottom-left fess-bottom-left))
                                                                                 :angle 180
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        {line-fess-top-left        :line
         line-fess-top-left-offset :start-offset
         :as                       line-fess-top-left-data}         (line/create line
                                                                                 (v/abs (v/- corner-top-left fess-top-left))
                                                                                 :reversed? true
                                                                                 :joint-angle 90
                                                                                 :render-options render-options)
        parts                                                       [[["M" (v/+ corner-top-left
                                                                                line-pale-top-left-offset)
                                                                       (line/stitch line-pale-top-left)
                                                                       (infinity/path :clockwise
                                                                                      [:top :top]
                                                                                      [(v/+ pale-top-left
                                                                                            line-pale-top-left-offset)
                                                                                       (v/+ pale-top-right
                                                                                            line-pale-top-right-offset)])
                                                                       (line/stitch line-pale-top-right)
                                                                       "L" (v/+ corner-top-right
                                                                                line-fess-top-right-offset)
                                                                       (line/stitch line-fess-top-right)
                                                                       (infinity/path :clockwise
                                                                                      [:right :right]
                                                                                      [(v/+ fess-top-right
                                                                                            line-fess-top-right-offset)
                                                                                       (v/+ fess-bottom-right
                                                                                            line-fess-bottom-right-offset)])
                                                                       (line/stitch line-fess-bottom-right)
                                                                       "L" (v/+ corner-bottom-right
                                                                                line-pale-bottom-right-offset)
                                                                       (line/stitch line-pale-bottom-right)
                                                                       (infinity/path :clockwise
                                                                                      [:bottom :bottom]
                                                                                      [(v/+ pale-bottom-right
                                                                                            line-pale-bottom-right-offset)
                                                                                       (v/+ pale-bottom-left
                                                                                            line-pale-bottom-left-offset)])
                                                                       (line/stitch line-pale-bottom-left)
                                                                       "L" (v/+ corner-bottom-left
                                                                                line-fess-bottom-left-offset)
                                                                       (line/stitch line-fess-bottom-left)
                                                                       (infinity/path :clockwise
                                                                                      [:left :left]
                                                                                      [(v/+ fess-bottom-left
                                                                                            line-fess-bottom-left-offset)
                                                                                       (v/+ fess-top-left
                                                                                            line-fess-top-left-offset)])
                                                                       (line/stitch line-fess-top-left)
                                                                       "z"]
                                                                      [top bottom left right]]]
        field                                                       (if (charge/counterchangable? field parent)
                                                                      (charge/counterchange-field field parent)
                                                                      field)
        [fimbriation-elements-1 fimbriation-outlines-1]             (render-fimbriation corner-top-left
                                                                                        pale-top-left
                                                                                        [:none :top]
                                                                                        line-pale-top-left-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-2 fimbriation-outlines-2]             (render-fimbriation pale-top-right
                                                                                        corner-top-right
                                                                                        [:top :none]
                                                                                        line-pale-top-right-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-3 fimbriation-outlines-3]             (render-fimbriation corner-top-right
                                                                                        fess-top-right
                                                                                        [:none :right]
                                                                                        line-fess-top-right-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-4 fimbriation-outlines-4]             (render-fimbriation fess-bottom-right
                                                                                        corner-bottom-right
                                                                                        [:right :none]
                                                                                        line-fess-bottom-right-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-5 fimbriation-outlines-5]             (render-fimbriation corner-bottom-right
                                                                                        pale-bottom-right
                                                                                        [:none :bottom]
                                                                                        line-pale-bottom-right-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-6 fimbriation-outlines-6]             (render-fimbriation pale-bottom-left
                                                                                        corner-bottom-left
                                                                                        [:bottom :none]
                                                                                        line-pale-bottom-left-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-7 fimbriation-outlines-7]             (render-fimbriation corner-bottom-left
                                                                                        fess-bottom-left
                                                                                        [:none :left]
                                                                                        line-fess-bottom-left-data
                                                                                        (:fimbriation line)
                                                                                        render-options)
        [fimbriation-elements-8 fimbriation-outlines-8]             (render-fimbriation fess-top-left
                                                                                        corner-top-left
                                                                                        [:left :none]
                                                                                        line-fess-top-left-data
                                                                                        (:fimbriation line)
                                                                                        render-options)]
    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     fimbriation-elements-3
     fimbriation-elements-4
     fimbriation-elements-5
     fimbriation-elements-6
     fimbriation-elements-7
     fimbriation-elements-8
     [division/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top-left
                               line-pale-top-left-offset)
                      (line/stitch line-pale-top-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-top-right
                               line-pale-top-right-offset)
                      (line/stitch line-pale-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top-right
                               line-fess-top-right-offset)
                      (line/stitch line-fess-top-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-bottom-right
                               line-fess-bottom-right-offset)
                      (line/stitch line-fess-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-right
                               line-pale-bottom-right-offset)
                      (line/stitch line-pale-bottom-right)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ pale-bottom-left
                               line-pale-bottom-left-offset)
                      (line/stitch line-pale-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom-left
                               line-fess-bottom-left-offset)
                      (line/stitch line-fess-bottom-left)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ fess-top-left
                               line-fess-top-left-offset)
                      (line/stitch line-fess-top-left)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4
         fimbriation-outlines-5
         fimbriation-outlines-6
         fimbriation-outlines-7
         fimbriation-outlines-8])
      environment ordinary context]]))

(defn saltire
  {:display-name "Saltire"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}   (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                 geometry
        points                                         (:points environment)
        origin-point                                   (position/calculate origin environment :fess)
        top                                            (assoc (:top points) :x (:x origin-point))
        bottom                                         (assoc (:bottom points) :x (:x origin-point))
        left                                           (assoc (:left points) :y (:y origin-point))
        right                                          (assoc (:right points) :y (:y origin-point))
        width                                          (:width environment)
        band-width                                     (-> width
                                                           (* size)
                                                           (/ 100))
        direction                                      (division/direction diagonal-mode points origin-point)
        diagonal-top-left                              (v/project-x origin-point (v/dot direction (v/v -1 -1)) (:x left))
        diagonal-top-right                             (v/project-x origin-point (v/dot direction (v/v 1 -1)) (:x right))
        diagonal-bottom-left                           (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right                          (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-top-left                                 (division/angle-to-point origin-point diagonal-top-left)
        angle-top-right                                (division/angle-to-point origin-point diagonal-top-right)
        angle-bottom-left                              (division/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                             (division/angle-to-point origin-point diagonal-bottom-right)
        angle                                          (-> angle-bottom-right (* Math/PI) (/ 180))
        dx                                             (/ band-width 2 (Math/sin angle))
        dy                                             (/ band-width 2 (Math/cos angle))
        offset-top                                     (v/v 0 (- dy))
        offset-bottom                                  (v/v 0 dy)
        offset-left                                    (v/v (- dx) 0)
        offset-right                                   (v/v dx 0)
        corner-top                                     (v/+ origin-point offset-top)
        corner-bottom                                  (v/+ origin-point offset-bottom)
        corner-left                                    (v/+ origin-point offset-left)
        corner-right                                   (v/+ origin-point offset-right)
        top-left-upper                                 (v/+ diagonal-top-left offset-top)
        top-left-lower                                 (v/+ diagonal-top-left offset-bottom)
        top-right-upper                                (v/+ diagonal-top-right offset-top)
        top-right-lower                                (v/+ diagonal-top-right offset-bottom)
        bottom-left-upper                              (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                              (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                             (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                             (v/+ diagonal-bottom-right offset-bottom)
        {line-top-left-lower        :line
         line-top-left-lower-offset :start-offset}     (line/create line
                                                                    (Math/abs (:y (v/- corner-left top-left-lower)))
                                                                    :angle  angle-top-left
                                                                    :render-options   render-options)
        {line-top-left-upper        :line
         line-top-left-upper-offset :start-offset}     (line/create line
                                                                    (v/abs (v/- corner-top top-left-upper))
                                                                    :angle     (- angle-top-left 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        {line-top-right-upper        :line
         line-top-right-upper-offset :start-offset}    (line/create line
                                                                    (v/abs (v/- corner-top top-right-upper))
                                                                    :angle angle-top-right
                                                                    :render-options render-options)
        {line-top-right-lower        :line
         line-top-right-lower-offset :start-offset}    (line/create line
                                                                    (v/abs (v/- corner-right top-right-lower))
                                                                    :angle (- angle-top-right 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        {line-bottom-right-upper        :line
         line-bottom-right-upper-offset :start-offset} (line/create line
                                                                    (v/abs (v/- corner-right bottom-right-upper))
                                                                    :angle angle-bottom-right
                                                                    :render-options render-options)
        {line-bottom-right-lower        :line
         line-bottom-right-lower-offset :start-offset} (line/create line
                                                                    (v/abs (v/- corner-bottom bottom-right-lower))
                                                                    :angle (- angle-bottom-right 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        {line-bottom-left-lower        :line
         line-bottom-left-lower-offset :start-offset}  (line/create line
                                                                    (v/abs (v/- corner-bottom bottom-left-lower))
                                                                    :angle angle-bottom-left
                                                                    :render-options render-options)
        {line-bottom-left-upper        :line
         line-bottom-left-upper-offset :start-offset}  (line/create line
                                                                    (v/abs (v/- corner-left bottom-left-upper))
                                                                    :angle (- angle-bottom-left 180)
                                                                    :reversed? true
                                                                    :render-options render-options)
        parts                                          [[["M" (v/+ corner-left
                                                                   line-top-left-lower-offset)
                                                          (line/stitch line-top-left-lower)
                                                          (infinity/path :clockwise
                                                                         [:left :left]
                                                                         [(v/+ top-left-lower
                                                                               line-top-left-lower-offset)
                                                                          (v/+ top-left-upper
                                                                               line-top-left-upper-offset)])
                                                          (line/stitch line-top-left-upper)
                                                          "L" (v/+ corner-top
                                                                   line-top-right-upper-offset)
                                                          (line/stitch line-top-right-upper)
                                                          (infinity/path :clockwise
                                                                         [:right :right]
                                                                         [(v/+ top-right-upper
                                                                               line-top-right-upper-offset)
                                                                          (v/+ top-right-lower
                                                                               line-top-right-lower-offset)])
                                                          (line/stitch line-top-right-lower)
                                                          "L" (v/+ corner-right
                                                                   line-bottom-right-upper-offset)
                                                          (line/stitch line-bottom-right-upper)
                                                          (infinity/path :clockwise
                                                                         [:right :right]
                                                                         [(v/+ bottom-right-upper
                                                                               line-bottom-right-upper-offset)
                                                                          (v/+ bottom-right-lower
                                                                               line-bottom-right-lower-offset)])
                                                          (line/stitch line-bottom-right-lower)
                                                          "L" (v/+ corner-bottom
                                                                   line-bottom-left-lower-offset)
                                                          (line/stitch line-bottom-left-lower)
                                                          (infinity/path :clockwise
                                                                         [:left :left]
                                                                         [(v/+ bottom-left-lower
                                                                               line-bottom-left-lower)
                                                                          (v/+ bottom-left-upper
                                                                               line-bottom-left-upper-offset)])
                                                          (line/stitch line-bottom-left-upper)
                                                          "z"]
                                                         [top bottom left right]]]
        field                                          (if (charge/counterchangable? field parent)
                                                         (charge/counterchange-field field parent)
                                                         field)]
    [division/make-division
     :ordinary-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g division/outline-style
        [:path {:d (svg/make-path
                    ["M" (v/+ corner-left
                              line-top-left-lower-offset)
                     (line/stitch line-top-left-lower)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ top-left-upper
                              line-top-left-upper-offset)
                     (line/stitch line-top-left-upper)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ corner-top
                              line-top-right-upper-offset)
                     (line/stitch line-top-right-upper)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ top-right-lower
                              line-top-right-lower-offset)
                     (line/stitch line-top-right-lower)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ corner-right
                              line-bottom-right-upper-offset)
                     (line/stitch line-bottom-right-upper)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom-right-lower
                              line-bottom-right-lower-offset)
                     (line/stitch line-bottom-right-lower)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ corner-bottom
                              line-bottom-left-lower-offset)
                     (line/stitch line-bottom-left-lower)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ bottom-left-upper
                              line-bottom-left-upper-offset)
                     (line/stitch line-bottom-left-upper)])}]])
     environment ordinary context]))

(defn chevron
  {:display-name "Chevron"}
  [{:keys [field hints] :as ordinary} parent environment {:keys [render-options] :as context}]
  (let [{:keys [line origin diagonal-mode geometry]}                  (options/sanitize ordinary (options ordinary))
        {:keys [size]}                                                geometry
        opposite-line                                                 (sanitize-opposite-line ordinary line)
        points                                                        (:points environment)
        origin-point                                                  (position/calculate origin environment :fess)
        top                                                           (assoc (:top points) :x (:x origin-point))
        bottom                                                        (assoc (:bottom points) :x (:x origin-point))
        left                                                          (assoc (:left points) :y (:y origin-point))
        right                                                         (assoc (:right points) :y (:y origin-point))
        height                                                        (:height environment)
        band-width                                                    (-> height
                                                                          (* size)
                                                                          (/ 100))
        direction                                                     (division/direction diagonal-mode points origin-point)
        diagonal-bottom-left                                          (v/project-x origin-point (v/dot direction (v/v -1 1)) (:x left))
        diagonal-bottom-right                                         (v/project-x origin-point (v/dot direction (v/v 1 1)) (:x right))
        angle-bottom-left                                             (division/angle-to-point origin-point diagonal-bottom-left)
        angle-bottom-right                                            (division/angle-to-point origin-point diagonal-bottom-right)
        angle                                                         (-> angle-bottom-right (* Math/PI) (/ 180))
        joint-angle                                                   (- angle-bottom-left angle-bottom-right)
        dy                                                            (/ band-width 2 (Math/cos angle))
        offset-top                                                    (v/v 0 (- dy))
        offset-bottom                                                 (v/v 0 dy)
        corner-top                                                    (v/+ origin-point offset-top)
        corner-bottom                                                 (v/+ origin-point offset-bottom)
        bottom-left-upper                                             (v/+ diagonal-bottom-left offset-top)
        bottom-left-lower                                             (v/+ diagonal-bottom-left offset-bottom)
        bottom-right-upper                                            (v/+ diagonal-bottom-right offset-top)
        bottom-right-lower                                            (v/+ diagonal-bottom-right offset-bottom)
        line                                                          (-> line
                                                                          (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                          (update-in [:fimbriation :thickness-2] (percent-of height)))
        opposite-line                                                 (-> opposite-line
                                                                          (update-in [:fimbriation :thickness-1] (percent-of height))
                                                                          (update-in [:fimbriation :thickness-2] (percent-of height)))
        {line-bottom-right-upper        :line
         line-bottom-right-upper-offset :start-offset
         :as                            line-bottom-right-upper-data} (line/create line
                                                                                   (v/abs (v/- corner-top bottom-right-upper))
                                                                                   :angle angle-bottom-right
                                                                                   :render-options render-options
                                                                                   :joint-angle (- joint-angle))
        {line-bottom-right-lower        :line
         line-bottom-right-lower-offset :start-offset
         :as                            line-bottom-right-lower-data} (line/create opposite-line
                                                                                   (v/abs (v/- corner-bottom bottom-right-lower))
                                                                                   :angle (- angle-bottom-right 180)
                                                                                   :reversed? true
                                                                                   :render-options render-options
                                                                                   :joint-angle joint-angle)
        {line-bottom-left-lower        :line
         line-bottom-left-lower-offset :start-offset
         :as                           line-bottom-left-lower-data}   (line/create opposite-line
                                                                                   (v/abs (v/- corner-bottom bottom-left-lower))
                                                                                   :angle angle-bottom-left
                                                                                   :render-options render-options
                                                                                   :joint-angle joint-angle)
        {line-bottom-left-upper        :line
         line-bottom-left-upper-offset :start-offset
         :as                           line-bottom-left-upper-data}   (line/create line
                                                                                   (v/abs (v/- corner-top bottom-left-upper))
                                                                                   :angle (- angle-bottom-left 180)
                                                                                   :reversed? true
                                                                                   :render-options render-options
                                                                                   :joint-angle (- joint-angle))
        parts                                                         [[["M" (v/+ corner-top
                                                                                  line-bottom-right-upper-offset)
                                                                         (line/stitch line-bottom-right-upper)
                                                                         (infinity/path :clockwise
                                                                                        [:right :right]
                                                                                        [(v/+ bottom-right-upper
                                                                                              line-bottom-right-upper-offset)
                                                                                         (v/+ bottom-right-lower
                                                                                              line-bottom-right-lower-offset)])
                                                                         (line/stitch line-bottom-right-lower)
                                                                         "L" (v/+ corner-bottom
                                                                                  line-bottom-left-lower-offset)
                                                                         (line/stitch line-bottom-left-lower)
                                                                         (infinity/path :clockwise
                                                                                        [:left :left]
                                                                                        [(v/+ bottom-left-lower
                                                                                              line-bottom-left-lower-offset)
                                                                                         (v/+ bottom-left-upper
                                                                                              line-bottom-left-upper-offset)])
                                                                         (line/stitch line-bottom-left-upper)
                                                                         "z"]
                                                                        [top bottom left right]]]
        field                                                         (if (charge/counterchangable? field parent)
                                                                        (charge/counterchange-field field parent)
                                                                        field)
        [fimbriation-elements-1 fimbriation-outlines-1]               (render-fimbriation corner-top
                                                                                          bottom-right-upper
                                                                                          [:none :bottom]
                                                                                          line-bottom-right-upper-data
                                                                                          (:fimbriation line)
                                                                                          render-options)
        [fimbriation-elements-2 fimbriation-outlines-2]               (render-fimbriation bottom-right-lower
                                                                                          corner-bottom
                                                                                          [:bottom :none]
                                                                                          line-bottom-right-lower-data
                                                                                          (:fimbriation opposite-line)
                                                                                          render-options)
        [fimbriation-elements-3 fimbriation-outlines-3]               (render-fimbriation corner-bottom
                                                                                          bottom-left-lower
                                                                                          [:none :bottom]
                                                                                          line-bottom-left-lower-data
                                                                                          (:fimbriation opposite-line)
                                                                                          render-options)
        [fimbriation-elements-4 fimbriation-outlines-4]               (render-fimbriation bottom-left-upper
                                                                                          corner-top
                                                                                          [:bottom :none]
                                                                                          line-bottom-left-upper-data
                                                                                          (:fimbriation line)
                                                                                          render-options)]
    [:<>
     fimbriation-elements-1
     fimbriation-elements-2
     fimbriation-elements-3
     fimbriation-elements-4
     [division/make-division
      :ordinary-pale [field] parts
      [:all]
      (when (or (:outline? render-options)
                (:outline? hints))
        [:g division/outline-style
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-top
                               line-bottom-right-upper-offset)
                      (line/stitch line-bottom-right-upper)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-right-lower
                               line-bottom-right-lower-offset)
                      (line/stitch line-bottom-right-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ corner-bottom
                               line-bottom-left-lower-offset)
                      (line/stitch line-bottom-left-lower)])}]
         [:path {:d (svg/make-path
                     ["M" (v/+ bottom-left-upper
                               line-bottom-left-upper-offset)
                      (line/stitch line-bottom-left-upper)])}]
         fimbriation-outlines-1
         fimbriation-outlines-2
         fimbriation-outlines-3
         fimbriation-outlines-4])
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
