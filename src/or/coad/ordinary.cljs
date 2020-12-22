(ns or.coad.ordinary
  (:require [or.coad.field-environment :as field-environment]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(def band-quotient 5)

(defn pale [{:keys [content line] :as field} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-pale")
        line-style (or (:style line) :straight)
        chief (get-in environment [:points :chief])
        base (get-in environment [:points :base])
        fess (get-in environment [:points :fess])
        width (:width environment)
        col1 (- (:x fess) (/ width band-quotient 2))
        col2 (+ (:x fess) (/ width band-quotient 2))
        first-chief (v/v col1 (:y chief))
        first-base (v/v col1 (:y base))
        second-chief (v/v col2 (:y chief))
        second-base (v/v col2 (:y base))
        {line :line} (line/create line-style
                                  (:y (v/- base chief))
                                  :flipped? true
                                  :angle 90)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:y (v/- base chief))
                                                    :angle -90
                                                    :reversed? true)
        second-base-adjusted (v/extend second-chief second-base line-reversed-length)
        ordinary-field (field-environment/create
                        (svg/make-path ["M" first-chief
                                        (line/stitch line)
                                        "L" first-base
                                        "L" second-base-adjusted
                                        (line/stitch line-reversed)
                                        "L" second-chief
                                        "z"])
                        {:parent field
                         :context [:pale]
                         :ordinary? true})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-field)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render content ordinary-field options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-chief
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-base-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn fess [{:keys [content line] :as field} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-fess")
        line-style (or (:style line) :straight)
        dexter (get-in environment [:points :dexter])
        sinister (get-in environment [:points :sinister])
        fess (get-in environment [:points :fess])
        height (:height environment)
        row1 (- (:y fess) (/ height band-quotient 2))
        row2 (+ (:y fess) (/ height band-quotient 2))
        first-dexter (v/v (:x dexter) row1)
        first-sinister (v/v (:x sinister) row1)
        second-dexter (v/v (:x dexter) row2)
        second-sinister (v/v (:x sinister) row2)
        {line :line} (line/create line-style
                                  (:x (v/- sinister dexter)))
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- sinister dexter))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180)
        second-sinister-adjusted (v/extend second-dexter second-sinister line-reversed-length)
        ordinary-field (field-environment/create
                        (svg/make-path ["M" first-dexter
                                        (line/stitch line)
                                        "L" first-sinister
                                        "L" second-sinister-adjusted
                                        (line/stitch line-reversed)
                                        "L" dexter
                                        "z"])
                        {:parent field
                         :context [:fess]
                         :ordinary? true})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-field)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render content ordinary-field options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" first-dexter
                     (line/stitch line)])}]
        [:path {:d (svg/make-path
                    ["M" second-sinister-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn chief [{:keys [content line] :as field} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-chief")
        line-style (or (:style line) :straight)
        top-left (get-in environment [:points :top-left])
        top-right (get-in environment [:points :top-right])
        dexter (get-in environment [:points :dexter])
        sinister (get-in environment [:points :sinister])
        height (:height environment)
        row (+ (:y chief) (/ height band-quotient))
        row-dexter (v/v (:x dexter) row)
        row-sinister (v/v (:x sinister) row)
        {line-reversed :line
         line-reversed-length :length} (line/create line-style
                                                    (:x (v/- sinister dexter))
                                                    :reversed? true
                                                    :flipped? true
                                                    :angle 180)
        row-sinister-adjusted (v/extend row-dexter row-sinister line-reversed-length)
        ordinary-field (field-environment/create
                        (svg/make-path ["M" top-left
                                        "L" top-right
                                        "L" row-sinister-adjusted
                                        (line/stitch line-reversed)
                                        "L" row-dexter
                                        "z"])
                        {:parent field
                         :context [:chief]
                         :ordinary? true})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-field)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render content ordinary-field options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-sinister-adjusted
                     (line/stitch line-reversed)])}]])]))

(defn base [{:keys [content line] :as field} environment top-level-render options]
  (let [mask-id (svg/id "ordinary-base")
        line-style (or (:style line) :straight)
        bottom-left (get-in environment [:points :bottom-left])
        bottom-right (get-in environment [:points :bottom-right])
        dexter (get-in environment [:points :dexter])
        sinister (get-in environment [:points :sinister])
        base (get-in environment [:points :base])
        height (:height environment)
        row (- (:y base) (/ height band-quotient))
        row-dexter (v/v (:x dexter) row)
        row-sinister (v/v (:x sinister) row)
        {line :line} (line/create line-style
                                  (:x (v/- sinister dexter)))
        ordinary-field (field-environment/create
                        (svg/make-path ["M" row-dexter
                                        (line/stitch line)
                                        "L" row-sinister
                                        "L" bottom-right
                                        "L" bottom-left
                                        "z"])
                        {:parent field
                         :meta {:context [:base]
                                :ordinary? true}})]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (:shape ordinary-field)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [top-level-render content ordinary-field options]]
     (when (:outline? options)
       [:g.outline
        [:path {:d (svg/make-path
                    ["M" row-dexter
                     (line/stitch line)])}]])]))

(defn bend [{:keys [content line] :as field} field top-level-render]
  [:<>])

(defn bend-sinister [{:keys [content line] :as field} field top-level-render]
  [:<>])

(defn cross [{:keys [content line] :as field} field top-level-render]
  [:<>])

(defn saltire [{:keys [content line] :as field} field top-level-render]
  [:<>])

(defn chevron [{:keys [content line] :as field} field top-level-render]
  [:<>])

(defn pall [{:keys [content line] :as field} field top-level-render]
  [:<>])

(def kinds
  [["Pale" :pale pale]
   ["Fess" :fess fess]
   ["Chief" :per-chevron chief]
   ["Base" :base base]
   ;; ["Bend" :bend bend]
   ;; ["Bend Sinister" :bend-sinister bend-sinister]
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
