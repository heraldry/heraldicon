(ns heraldry.coat-of-arms.division.type.tierced-per-pale
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Tierced per pale"
   :value        :tierced-per-pale
   :parts        ["dexter" "fess" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line layout origin]}      (options/sanitize division (division-options/options division))
        {:keys [stretch-x]}               layout
        points                            (:points environment)
        origin-point                      (position/calculate origin environment :fess)
        top                               (assoc (:top points) :x (:x origin-point))
        top-left                          (:top-left points)
        bottom                            (assoc (:bottom points) :x (:x origin-point))
        bottom-right                      (:bottom-right points)
        width                             (:width environment)
        middle-half-width                 (-> width
                                              (/ 6)
                                              (* stretch-x))
        col1                              (- (:x origin-point) middle-half-width)
        col2                              (+ (:x origin-point) middle-half-width)
        first-top                         (v/v col1 (:y top))
        first-bottom                      (v/v col1 (:y bottom))
        second-top                        (v/v col2 (:y top))
        second-bottom                     (v/v col2 (:y bottom))
        {line-one       :line
         line-one-start :line-start}      (line/create line
                                                       (:y (v/- bottom top))
                                                       :angle 90
                                                       :render-options render-options)
        {line-reversed       :line
         line-reversed-start :line-start} (line/create line
                                                       (:y (v/- bottom top))
                                                       :angle -90
                                                       :reversed? true
                                                       :flipped? true
                                                       :render-options render-options)
        parts                             [[["M" (v/+ first-top
                                                      line-one-start)
                                             (svg/stitch line-one)
                                             (infinity/path :clockwise
                                                            [:bottom :top]
                                                            [(v/+ first-bottom
                                                                  line-one-start)
                                                             (v/+ first-top
                                                                  line-one-start)])
                                             "z"]
                                            [top-left
                                             first-bottom]]

                                           [["M" (v/+ second-bottom
                                                      line-reversed-start)
                                             (svg/stitch line-reversed)
                                             (infinity/path :counter-clockwise
                                                            [:top :top]
                                                            [(v/+ second-top
                                                                  line-reversed-start)
                                                             (v/+ first-top
                                                                  line-one-start)])
                                             (svg/stitch line-one)
                                             (infinity/path :counter-clockwise
                                                            [:bottom :bottom]
                                                            [(v/+ first-top
                                                                  line-one-start)
                                                             (v/+ second-bottom
                                                                  line-reversed-start)
                                                             first-bottom second-bottom])
                                             "z"]
                                            [first-top
                                             second-bottom]]

                                           [["M" (v/+ second-bottom
                                                      line-reversed-start)
                                             (svg/stitch line-reversed)
                                             (infinity/path :clockwise
                                                            [:top :bottom]
                                                            [(v/+ second-top
                                                                  line-reversed-start)
                                                             (v/+ second-bottom
                                                                  line-reversed-start)])
                                             "z"]
                                            [second-top
                                             bottom-right]]]]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all
       [(svg/make-path
         ["M" (v/+ second-bottom
                   line-reversed-start)
          (svg/stitch line-reversed)])]
       nil]
      environment division context]
     (when (or (:outline? render-options)
               (:outline? hints))
       [:g outline/style
        [:path {:d (svg/make-path
                    ["M" (v/+ first-top
                              line-one-start)
                     (svg/stitch line-one)])}]
        [:path {:d (svg/make-path
                    ["M" (v/+ second-bottom
                              line-reversed-start)
                     (svg/stitch line-reversed)])}]])]))
