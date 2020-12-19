(ns or.coad.division
  (:require [or.coad.field :as field]
            [or.coad.line :as line]
            [or.coad.svg :as svg]
            [or.coad.vector :as v]))

(defn per-pale [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-pale-1_")
        mask-id-2 (svg/id "division-pale-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        base (get-in field [:points :base])
        {line :line} (line/create line-style
                                  (:y (v/- base chief))
                                  :angle -90)
        field-1 (field/make-field
                 (svg/make-path ["M" base
                                 (line/stitch line)
                                 "L" top-left
                                 "L" bottom-left
                                 "z"])
                 {:parent field
                  :context [:per-pale :left]})
        field-2 (field/make-field
                 (svg/make-path ["M" base
                                 (line/stitch line)
                                 "L" top-right
                                 "L" bottom-right
                                 "z"])
                 {:parent field
                  :meta {:context [:per-pale :left]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]]))

(defn per-fess [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-fess-1_")
        mask-id-2 (svg/id "division-fess-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        dexter (get-in field [:points :dexter])
        sinister (get-in field [:points :sinister])
        {line :line} (line/create line-style
                                  (:x (v/- sinister dexter)))
        field-1 (field/make-field
                 (svg/make-path ["M" dexter
                                 (line/stitch line)
                                 "L" top-right
                                 "L" top-left
                                 "z"])
                 {:parent field
                  :context [:per-fess :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" dexter
                                 (line/stitch line)
                                 "L" bottom-right
                                 "L" bottom-left
                                 "z"])
                 {:parent field
                  :meta {:context [:per-fess :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]]))

(defn per-bend [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-bend-1_")
        mask-id-2 (svg/id "division-bend-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        bend-intersection (v/project top-left fess (:x top-right))
        {line :line} (line/create line-style
                                  (v/abs (v/- bend-intersection top-left))
                                  :angle 45)
        field-1 (field/make-field
                 (svg/make-path ["M" top-left
                                 (line/stitch line)
                                 "L" top-right
                                 "z"])
                 {:parent field
                  :context [:per-bend :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" top-left
                                 (line/stitch line)
                                 "L" bottom-right
                                 "L" bottom-left
                                 "z"])
                 {:parent field
                  :meta {:context [:per-bend :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]]))

(defn per-bend-sinister [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-bend-sinister-1_")
        mask-id-2 (svg/id "division-bend-sinister-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        bend-intersection (v/project top-right fess (:x top-left))
        {line :line} (line/create line-style
                                  (v/abs (v/- bend-intersection top-right))
                                  :angle 135
                                  :flipped? true)
        field-1 (field/make-field
                 (svg/make-path ["M" top-right
                                 (line/stitch line)
                                 "L" top-left
                                 "z"])
                 {:parent field
                  :context [:per-bend-sinister :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" top-right
                                 (line/stitch line)
                                 "L" bottom-left
                                 "L" bottom-right
                                 "z"])
                 {:parent field
                  :meta {:context [:per-bend-sinister :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]]))

(defn per-chevron [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-chevron-1_")
        mask-id-2 (svg/id "division-chevron-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        bend-intersection-dexter (v/project top-right fess (:x top-left))
        bend-intersection-sinister (v/project top-left fess (:x top-right))
        {line-dexter :line
         line-dexter-length :length} (line/create line-style
                                                  (v/abs (v/- bend-intersection-dexter fess))
                                                  :angle -45
                                                  :reversed? true)
        {line-sinister :line} (line/create line-style
                                           (v/abs (v/- bend-intersection-sinister fess))
                                           :angle 45)
        bend-intersection-dexter-adjusted (v/extend fess bend-intersection-dexter line-dexter-length)
        field-1 (field/make-field
                 (svg/make-path ["M" fess
                                 (line/stitch line-sinister)
                                 "L" top-right
                                 "L" top-left
                                 "L" bend-intersection-dexter-adjusted
                                 (line/stitch line-dexter)
                                 "z"])
                 {:parent field
                  :context [:per-chevron :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" fess
                                 (line/stitch line-sinister)
                                 "L" bottom-right
                                 "L" bottom-left
                                 "L" bend-intersection-dexter-adjusted
                                 (line/stitch line-dexter)
                                 "z"])
                 {:parent field
                  :meta {:context [:per-chevron :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]]))

(defn per-saltire [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-saltire-1_")
        mask-id-2 (svg/id "division-saltire-2_")
        mask-id-3 (svg/id "division-saltire-3_")
        mask-id-4 (svg/id "division-saltire-4_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        bend-intersection-sinister (v/project top-left fess (:x top-right))
        bend-intersection-dexter (v/project top-right fess (:x top-left))
        {line-chief-dexter :line
         line-chief-dexter-length :length} (line/create line-style
                                                        (v/abs (v/- top-left fess))
                                                        :angle 45
                                                        :reversed? true)
        {line-chief-sinister :line} (line/create line-style
                                                 (v/abs (v/- top-right fess))
                                                 :angle -45
                                                 :flipped? true)
        {line-base-sinister :line
         line-base-sinister-length :length} (line/create line-style
                                                         (v/abs (v/- bend-intersection-sinister fess))
                                                         :angle 225
                                                         :reversed? true)
        {line-base-dexter :line} (line/create line-style
                                              (v/abs (v/- bend-intersection-dexter fess))
                                              :angle -225
                                              :flipped? true)
        top-left-adjusted (v/extend
                           fess
                            top-left
                            line-chief-dexter-length)
        bend-intersection-sinister-adjusted (v/extend
                                             fess
                                              bend-intersection-sinister
                                              line-base-sinister-length)
        field-1 (field/make-field
                 (svg/make-path ["M" top-left-adjusted
                                 (line/stitch line-chief-dexter)
                                 "L" fess
                                 (line/stitch line-chief-sinister)
                                 "L" top-right
                                 "z"])
                 {:parent field
                  :context [:per-saltire :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" fess
                                 (line/stitch line-chief-sinister)
                                 "L" bend-intersection-sinister-adjusted
                                 (line/stitch line-base-sinister)
                                 "z"])
                 {:parent field
                  :meta {:context [:per-saltire :right]}})
        field-3 (field/make-field
                 (svg/make-path ["M" bend-intersection-sinister-adjusted
                                 (line/stitch line-base-sinister)
                                 "L" fess
                                 (line/stitch line-base-dexter)
                                 "L" bottom-left
                                 "L" bottom-right
                                 "z"])
                 {:parent field
                  :meta {:context [:per-saltire :bottom]}})
        field-4 (field/make-field
                 (svg/make-path ["M" fess
                                 (line/stitch line-base-dexter)
                                 "L" top-left-adjusted
                                 (line/stitch line-chief-dexter)
                                 "z"])
                 {:parent field
                  :meta {:context [:per-saltire :left]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]
      [:mask {:id mask-id-4}
       [:path {:d (:shape field-4)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (first parts) field-3]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (second parts) field-4]]]))

(defn quarterly [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-quarterly-1_")
        mask-id-2 (svg/id "division-quarterly-2_")
        mask-id-3 (svg/id "division-quarterly-3_")
        mask-id-4 (svg/id "division-quarterly-4_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        base (get-in field [:points :base])
        fess (get-in field [:points :fess])
        dexter (get-in field [:points :dexter])
        sinister (get-in field [:points :sinister])
        {line-chief :line
         line-chief-length :length} (line/create line-style
                                                 (v/abs (v/- chief fess))
                                                 :angle 90
                                                 :reversed? true)
        {line-sinister :line} (line/create line-style
                                           (v/abs (v/- sinister fess))
                                           :flipped? true)
        {line-base :line
         line-base-length :length} (line/create line-style
                                                (v/abs (v/- base fess))
                                                :angle -90
                                                :reversed? true)
        {line-dexter :line} (line/create line-style
                                         (v/abs (v/- dexter fess))
                                         :angle -180
                                         :flipped? true)
        chief-adjusted (v/extend fess chief line-chief-length)
        base-adjusted (v/extend fess base line-base-length)
        field-1 (field/make-field (svg/make-path ["M" chief-adjusted
                                                  (line/stitch line-chief)
                                                  "L" fess
                                                  (line/stitch line-dexter)
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :context [:per-quarterly :top-left]})
        field-2 (field/make-field (svg/make-path ["M" chief-adjusted
                                                  (line/stitch line-chief)
                                                  "L" fess
                                                  (line/stitch line-sinister)
                                                  "L" top-right
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-quarterly :top-right]}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  (line/stitch line-sinister)
                                                  "L" bottom-right
                                                  "L" base-adjusted
                                                  (line/stitch line-base)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-quarterly :bottom-right]}})
        field-4 (field/make-field (svg/make-path ["M" base-adjusted
                                                  (line/stitch line-base)
                                                  "L" fess
                                                  (line/stitch line-dexter)
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-quarterly :bottom-left]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]
      [:mask {:id mask-id-4}
       [:path {:d (:shape field-4)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (first parts) field-3]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (second parts) field-4]]]))

(defn gyronny [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-gyronny-1_")
        mask-id-2 (svg/id "division-gyronny-2_")
        mask-id-3 (svg/id "division-gyronny-3_")
        mask-id-4 (svg/id "division-gyronny-4_")
        mask-id-5 (svg/id "division-gyronny-5_")
        mask-id-6 (svg/id "division-gyronny-6_")
        mask-id-7 (svg/id "division-gyronny-7_")
        mask-id-8 (svg/id "division-gyronny-8_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        base (get-in field [:points :base])
        fess (get-in field [:points :fess])
        dexter (get-in field [:points :dexter])
        sinister (get-in field [:points :sinister])
        {line-chief :line
         line-chief-length :length} (line/create line-style
                                                 (v/abs (v/- chief fess))
                                                 :angle 90
                                                 :reversed? true)
        {line-sinister :line
         line-sinister-length :length} (line/create line-style
                                                    (v/abs (v/- sinister fess))
                                                    :reversed? true
                                                    :angle 180)
        {line-base :line
         line-base-length :length} (line/create line-style
                                                (v/abs (v/- base fess))
                                                :angle -90
                                                :reversed? true)
        {line-dexter :line
         line-dexter-length :length} (line/create line-style
                                                  (v/abs (v/- dexter fess))
                                                  :reversed? true)
        chief-adjusted (v/extend fess chief line-chief-length)
        base-adjusted (v/extend fess base line-base-length)
        dexter-adjusted (v/extend fess dexter line-dexter-length)
        sinister-adjusted (v/extend fess sinister line-sinister-length)
        bend-intersection-dexter (v/project top-right fess (:x top-left))
        bend-intersection-sinister (v/project top-left fess (:x top-right))
        {line-chief-dexter :line} (line/create line-style
                                               (v/abs (v/- top-left fess))
                                               :flipped? true
                                               :angle -135)
        {line-chief-sinister :line} (line/create line-style
                                                 (v/abs (v/- top-right fess))
                                                 :flipped? true
                                                 :angle -45)
        {line-base-sinister :line} (line/create line-style
                                                (v/abs (v/- bend-intersection-sinister fess))
                                                :flipped? true
                                                :angle 45)
        {line-base-dexter :line} (line/create line-style
                                              (v/abs (v/- bend-intersection-dexter fess))
                                              :flipped? true
                                              :angle -225)

        field-1 (field/make-field (svg/make-path ["M" fess
                                                  (line/stitch line-chief-dexter)
                                                  "L" chief-adjusted
                                                  (line/stitch line-chief)
                                                  "z"])
                                  {:parent field
                                   :context [:per-gyronny :one]})
        field-2 (field/make-field (svg/make-path ["M" chief-adjusted
                                                  (line/stitch line-chief)
                                                  "L" fess
                                                  (line/stitch line-chief-sinister)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :two]}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  (line/stitch line-chief-sinister)
                                                  "L" sinister-adjusted
                                                  (line/stitch line-sinister)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :three]}})
        field-4 (field/make-field (svg/make-path ["M" sinister-adjusted
                                                  (line/stitch line-sinister)
                                                  "L" fess
                                                  (line/stitch line-base-sinister)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :four]}})
        field-5 (field/make-field (svg/make-path ["M" fess
                                                  (line/stitch line-base-sinister)
                                                  "L" bottom-right
                                                  "L" base-adjusted
                                                  (line/stitch line-base)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :five]}})
        field-6 (field/make-field (svg/make-path ["M" base-adjusted
                                                  (line/stitch line-base)
                                                  "L" fess
                                                  (line/stitch line-base-dexter)
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :six]}})
        field-7 (field/make-field (svg/make-path ["M" fess
                                                  (line/stitch line-base-dexter)
                                                  "L" dexter-adjusted
                                                  (line/stitch line-dexter)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :seven]}})
        field-8 (field/make-field (svg/make-path ["M" dexter-adjusted
                                                  (line/stitch line-dexter)
                                                  "L" fess
                                                  (line/stitch line-chief-dexter)
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-gyronny :eight]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]
      [:mask {:id mask-id-4}
       [:path {:d (:shape field-4)
               :fill "#fff"}]]
      [:mask {:id mask-id-5}
       [:path {:d (:shape field-5)
               :fill "#fff"}]]
      [:mask {:id mask-id-6}
       [:path {:d (:shape field-6)
               :fill "#fff"}]]
      [:mask {:id mask-id-7}
       [:path {:d (:shape field-7)
               :fill "#fff"}]]
      [:mask {:id mask-id-8}
       [:path {:d (:shape field-8)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (first parts) field-3]]
     [:g {:mask (str "url(#" mask-id-4 ")")}
      [top-level-render (second parts) field-4]]
     [:g {:mask (str "url(#" mask-id-5 ")")}
      [top-level-render (first parts) field-5]]
     [:g {:mask (str "url(#" mask-id-6 ")")}
      [top-level-render (second parts) field-6]]
     [:g {:mask (str "url(#" mask-id-7 ")")}
      [top-level-render (first parts) field-7]]
     [:g {:mask (str "url(#" mask-id-8 ")")}
      [top-level-render (second parts) field-8]]]))

(defn tierced-in-pale [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-tierced-pale-1_")
        mask-id-2 (svg/id "division-tierced-pale-2_")
        mask-id-3 (svg/id "division-tierced-pale-3_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        base (get-in field [:points :base])
        fess (get-in field [:points :fess])
        width (:width field)
        col1 (- (:x fess) (/ width 6))
        col2 (+ (:x fess) (/ width 6))
        first-chief (v/v col1 (:y chief))
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
        field-1 (field/make-field
                 (svg/make-path ["M" first-chief
                                 (line/stitch line)
                                 "L" bottom-left
                                 "L" top-left
                                 "z"])
                 {:parent field
                  :context [:per-tierced-pale :left]})
        field-2 (field/make-field
                 (svg/make-path ["M" first-chief
                                 (line/stitch line)
                                 "L" second-base-adjusted
                                 (line/stitch line-reversed)
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-pale :middle]}})
        field-3 (field/make-field
                 (svg/make-path ["M" second-base-adjusted
                                 (line/stitch line-reversed)
                                 "L" top-right
                                 "L" bottom-right
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-pale :right]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (nth parts 2) field-3]]]))

(defn tierced-in-fesse [parts field top-level-render {:keys [line-style]}]
  (let [mask-id-1 (svg/id "division-tierced-pale-1_")
        mask-id-2 (svg/id "division-tierced-pale-2_")
        mask-id-3 (svg/id "division-tierced-pale-3_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        dexter (get-in field [:points :dexter])
        sinister (get-in field [:points :sinister])
        fess (get-in field [:points :fess])
        height (:height field)
        row1 (- (:y fess) (/ height 6))
        row2 (+ (:y fess) (/ height 6))
        first-dexter (v/v (:x dexter) row1)
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
        field-1 (field/make-field
                 (svg/make-path ["M" first-dexter
                                 (line/stitch line)
                                 "L" top-right
                                 "L" top-left
                                 "z"])
                 {:parent field
                  :context [:per-tierced-fesse :top]})
        field-2 (field/make-field
                 (svg/make-path ["M" first-dexter
                                 (line/stitch line)
                                 "L" second-sinister-adjusted
                                 (line/stitch line-reversed)
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-fesse :middle]}})
        field-3 (field/make-field
                 (svg/make-path ["M" second-sinister-adjusted
                                 (line/stitch line-reversed)
                                 "L" bottom-left
                                 "L" bottom-right
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-fesse :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (nth parts 2) field-3]]]))

(defn tierced-in-pairle [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-tierced-pairle-1_")
        mask-id-2 (svg/id "division-tierced-pairle-2_")
        mask-id-3 (svg/id "division-tierced-pairle-3_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        base (get-in field [:points :base])
        fess (get-in field [:points :fess])
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" fess
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :context [:per-tierced-pairle :top]})
        field-2 (field/make-field (svg/make-path ["M" fess
                                                  "L" top-right
                                                  "L" bottom-right
                                                  "L" base
                                                  "L" fess
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-tierced-pairle :right]}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  "L" base
                                                  "L" bottom-left
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context [:per-tierced-pall :left]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (nth parts 2) field-3]]]))

(defn tierced-in-pairle-reversed [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-tierced-pairle-reversed-1_")
        mask-id-2 (svg/id "division-tierced-pairle-reversed-2_")
        mask-id-3 (svg/id "division-tierced-pairle-reversed-3_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        fess (get-in field [:points :fess])
        width (:width field)
        fess-x-rel-dexter (- (:x fess) (:x top-left))
        fess-y-rel-dexter (- (:y fess) (:y top-left))
        fess-dir-dexter (v/v 1 (/ fess-y-rel-dexter fess-x-rel-dexter))
        bend-intersection-sinister (v/v (* (:x fess-dir-dexter) width)
                                        (* (:y fess-dir-dexter) width))
        fess-x-rel-sinister (- (:x fess) (:x top-right))
        fess-y-rel-sinister (- (:y fess) (:y top-right))
        fess-dir-sinister (v/v 1 (/ fess-y-rel-sinister fess-x-rel-sinister))
        bend-intersection-dexter (v/+
                                  top-right
                                  (v/v (* (:x fess-dir-sinister) (- width))
                                       (* (:y fess-dir-sinister) (- width))))
        field-1 (field/make-field
                 (svg/make-path ["M" top-left
                                 "L" chief
                                 "L" fess
                                 "L" bend-intersection-dexter
                                 "z"])
                 {:parent field
                  :context [:per-tierced-pairle-reversed :left]})
        field-2 (field/make-field
                 (svg/make-path ["M" chief
                                 "L" top-right
                                 "L" bend-intersection-sinister
                                 "L" fess
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-pairle-reversed :right]}})
        field-3 (field/make-field
                 (svg/make-path ["M" fess
                                 "L" bend-intersection-sinister
                                 "L" bottom-right
                                 "L" bottom-left
                                 "L" bend-intersection-dexter
                                 "z"])
                 {:parent field
                  :meta {:context [:per-tierced-pall-reversed :bottom]}})]
    [:<>
     [:defs
      [:mask {:id mask-id-1}
       [:path {:d (:shape field-1)
               :fill "#fff"}]]
      [:mask {:id mask-id-2}
       [:path {:d (:shape field-2)
               :fill "#fff"}]]
      [:mask {:id mask-id-3}
       [:path {:d (:shape field-3)
               :fill "#fff"}]]]
     [:g {:mask (str "url(#" mask-id-1 ")")}
      [top-level-render (first parts) field-1]]
     [:g {:mask (str "url(#" mask-id-2 ")")}
      [top-level-render (second parts) field-2]]
     [:g {:mask (str "url(#" mask-id-3 ")")}
      [top-level-render (nth parts 2) field-3]]]))

(def kinds
  [["Per Pale" :per-pale per-pale]
   ["Per Fess" :per-fess per-fess]
   ["Per Bend" :per-bend per-bend]
   ["Per Bend Sinister" :per-bend-sinister per-bend-sinister]
   ["Per Chevron" :per-chevron per-chevron]
   ["Per Saltire" :per-saltire per-saltire]
   ["Quarterly" :quarterly quarterly]
   ["Gyronny" :gyronny gyronny]
   ["Tierced in Pale" :tierced-in-pale tierced-in-pale]
   ["Tierced in Fesse" :tierced-in-fesse tierced-in-fesse]
   ["Tierced in Pairle" :tierced-in-pairle tierced-in-pairle]
   ["Tierced in Pairle Reversed" :tierced-in-pairle-reversed tierced-in-pairle-reversed]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn render [{:keys [type parts] :as division} field top-level-render]
  (let [function (get kinds-function-map type)]
    [function parts field top-level-render division]))
