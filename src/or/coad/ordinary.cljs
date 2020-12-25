(ns or.coad.ordinary
  (:require [or.coad.charge :as charge]
            [or.coad.division :as division]
            [or.coad.infinity :as infinity]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(def band-quotient 5)

(defn pale [{:keys [field line] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
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
                 (infinity/path :counter-clockwise
                                [:bottom :bottom]
                                [first-bottom second-bottom-adjusted])
                 (line/stitch line-reversed)
                 (infinity/path :counter-clockwise
                                [:top :top]
                                [second-top first-top])
                 "z"]
                [first-top second-bottom]]]
        field (if (:counterchanged? field)
                (assoc (charge/counterchange-field parent) :counterchanged? true)
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

(defn fess [{:keys [field line] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
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
                 (infinity/path :clockwise
                                [:right :right]
                                [first-right second-right-adjusted])
                 (line/stitch line-reversed)
                 (infinity/path :clockwise
                                [:left :left]
                                [second-left first-left])
                 "z"]
                [first-right second-left]]]
        field (if (:counterchanged? field)
                (assoc (charge/counterchange-field parent) :counterchanged? true)
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

(defn chief [{:keys [field line] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
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
        field (if (:counterchanged? field)
                (assoc (charge/counterchange-field parent) :counterchanged? true)
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

(defn base [{:keys [field line] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [line-style (or (:style line) :straight)
        bottom-right (get-in environment [:points :bottom-right])
        left (get-in environment [:points :left])
        right (get-in environment [:points :right])
        bottom (get-in environment [:points :bottom])
        height (:height environment)
        row (- (:y bottom) (/ height band-quotient))
        row-left (v/v (:x left) row)
        row-right (v/v (:x right) row)
        {line :line} (line/create line-style
                                  (:x (v/- right left))
                                  :options options)
        parts [[["M" row-left
                 (line/stitch line)
                 (infinity/path :clockwise
                                [:right :left]
                                [row-right row-left])
                 "z"]
                [row-left bottom-right]]]
        field (if (:counterchanged? field)
                (assoc (charge/counterchange-field parent) :counterchanged? true)
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

(defn bend [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
  [:<>])

(defn bend-right [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
  [:<>])

(defn cross [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
  [:<>])

(defn saltire [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
  [:<>])

(defn chevron [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
  [:<>])

(defn pall [{:keys [field line] :as ordinary} field top-level-render & {:keys [db-path]}]
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
              [name key]))))

(defn render [{:keys [type] :as ordinary} parent environment top-level-render options & {:keys [db-path]}]
  (let [function (get kinds-function-map type)]
    [function ordinary parent environment top-level-render options :db-path db-path]))
