(ns or.coad.ordinary
  (:require [or.coad.field-environment :as field-environment]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(def band-quotient 5)

(defn pale [{:keys [field line] :as ordinary} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-pale")
        line-style (or (:style line) :straight)
        top (get-in environment [:points :top])
        bottom (get-in environment [:points :bottom])
        fess (get-in environment [:points :fess])
        width (:width environment)
        col1 (- (:x fess) (/ width band-quotient 2))
        col2 (+ (:x fess) (/ width band-quotient 2))
        first-top (v/v col1 (:y top))
        first-bottom (v/v col1 (:y bottom))
        second-top (v/v col2 (:y top))
        second-bottom (v/v col2 (:y bottom))
        {line :line} (line/create line-style
                                  (:y (v/- bottom top))
                                  :flipped? true
                                  :angle 90)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:y (v/- bottom top))
                                                    :angle -90
                                                    :reversed? true)
        second-bottom-adjusted (v/extend second-top second-bottom line-reversed-length)
        ordinary-environment (field-environment/create
                              (svg/make-path ["M" first-top
                                              (line/stitch line)
                                              (infinity/path :counter-clockwise
                                                             [:bottom :bottom]
                                                             [first-bottom second-bottom-adjusted])
                                              (line/stitch line-reversed)
                                              (infinity/path :counter-clockwise
                                                             [:top :top]
                                                             [second-top first-top])
                                              "z"])
                              {:parent ordinary
                               :context [:pale]
                               :ordinary? true
                               :bounding-box (svg/bounding-box
                                              [first-top second-bottom])})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-environment)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render field ordinary-environment options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-top
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-bottom-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn fess [{:keys [field line] :as ordinary} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-fess")
        line-style (or (:style line) :straight)
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        fess (get-in environment [:points :fess])
        height (:height environment)
        row1 (- (:y fess) (/ height band-quotient 2))
        row2 (+ (:y fess) (/ height band-quotient 2))
        first-left (v/v (:x left) row1)
        first-right (v/v (:x right) row1)
        second-left (v/v (:x left) row2)
        second-right (v/v (:x right) row2)
        {line :line} (line/create line-style
                                  (:x (v/- right left)))
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180)
        second-right-adjusted (v/extend second-left second-right line-reversed-length)
        ordinary-environment (field-environment/create
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
                              {:parent ordinary
                               :context [:fess]
                               :ordinary? true
                               :bounding-box (svg/bounding-box
                                              [first-right second-left])})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-environment)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render field ordinary-environment options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-left
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-right-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn chief [{:keys [field line] :as ordinary} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-top")
        line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top (get-in environment [:points :top])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        height (:height environment)
        row (+ (:y top) (/ height band-quotient))
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- right left))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180)
        row-right-adjusted (v/extend row-left row-right line-reversed-length)
        ordinary-environment (field-environment/create
                              (svg/make-path ["M" row-right-adjusted
                                              (line/stitch line-reversed)
                                              (infinity/path :clockwise
                                                             [:left :right]
                                                             [row-left row-right-adjusted])
                                              "z"])
                              {:parent ordinary
                               :context [:top]
                               :ordinary? true
                               :bounding-box (svg/bounding-box
                                              [top-left row-right])})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-environment)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render field ordinary-environment options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-right-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn base [{:keys [field line] :as ordinary} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-bottom")
        line-style (or (:style line) :straight)
        bottom-right (get-in environment [:points :bottom-right])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        bottom (get-in environment [:points :bottom])
        height (:height environment)
        row (- (:y bottom) (/ height band-quotient))
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        {line :line} (line/create line-style
                                  (:x (v/- right left)))
        ordinary-environment (field-environment/create
                              (svg/make-path ["M" row-left
                                              (line/stitch line)
                                              (infinity/path :clockwise
                                                             [:right :left]
                                                             [row-right row-left])
                                              "z"])
                              {:parent ordinary
                               :context [:bottom]
                               :ordinary? true
                               :bounding-box (svg/bounding-box
                                              [row-left bottom-right])})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-environment)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render field ordinary-environment options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-left
                     (line/stitch line)])}]])]))

(defn bend [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(defn bend-right [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(defn cross [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(defn saltire [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(defn chevron [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(defn pall [{:keys [field line] :as ordinary} field top-level-render]
  [:<>])

(def kinds
  [["Pale" :pale pale]
   ["Fess" :fess fess]
   ["Chief" :chief chief]
   ["Base" :base base]
   ;; ["Bend" :bend bend]
   ;; ["Bend Sinister" :bend-right bend-right]
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
              [key name]))))

(defn render [{:keys [type] :as ordinary} environment top-level-render options]
  (let [function (get kinds-function-map type)]
    [function ordinary environment top-level-render options]))
