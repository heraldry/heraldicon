(ns or.coad.division
  (:require [or.coad.field :as field]
            [or.coad.svg :as svg]
            [or.coad.tincture :refer [tinctures]]))

(defn base-area [fill]
  [:rect {:x -1000
          :y -1000
          :width 2000
          :height 2000
          :fill fill}])

(def overlap 0.1)

(defn per-pale [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-pale-1_")
        mask-id-2 (svg/id "division-pale-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        chief (get-in field [:points :chief])
        base (get-in field [:points :base])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" (svg/translate chief [overlap 0])
                                                  "L" (svg/translate base [overlap 0])
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-pale :left)})
        field-2 (field/make-field (svg/make-path ["M" chief
                                                  "L" top-right
                                                  "L" bottom-right
                                                  "L" base
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-pale :left)}})]
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

(defn per-fess [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-fess-1_")
        mask-id-2 (svg/id "division-fess-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        dexter (get-in field [:points :dexter])
        sinister (get-in field [:points :sinister])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate sinister [0 overlap])
                                                  "L" (svg/translate dexter [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-fess :top)})
        field-2 (field/make-field (svg/make-path ["M" dexter
                                                  "L" sinister
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-fess :bottom)}})]
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

(defn per-bend [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-bend-1_")
        mask-id-2 (svg/id "division-bend-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        fess-x-rel (- (first fess) (first top-left))
        fess-y-rel (- (second fess) (second top-left))
        width (:width field)
        fess-dir [1 (/ fess-y-rel fess-x-rel)]
        bend-intersection [(* (first fess-dir) width)
                           (* (second fess-dir) width)]
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate bend-intersection [0 overlap])
                                                  "L" (svg/translate top-left [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-bend :top)})
        field-2 (field/make-field (svg/make-path ["M" top-left
                                                  "L" bend-intersection
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-bend :bottom)}})]
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

(defn per-bend-sinister [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-bend-sinister-1_")
        mask-id-2 (svg/id "division-bend-sinister-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        fess (get-in field [:points :fess])
        fess-x-rel (- (first fess) (first top-right))
        fess-y-rel (- (second fess) (second top-right))
        width (:width field)
        fess-dir [1 (/ fess-y-rel fess-x-rel)]
        bend-intersection (svg/translate
                           top-right
                           [(* (first fess-dir) (- width))
                            (* (second fess-dir) (- width))])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate top-right [0 overlap])
                                                  "L" (svg/translate bend-intersection [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-bend-sinister :top)})
        field-2 (field/make-field (svg/make-path ["M" top-right
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "L" bend-intersection
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-bend-sinister :bottom)}})]
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

(defn per-chevron [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-chevron-1_")
        mask-id-2 (svg/id "division-chevron-2_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        width (:width field)
        fess (get-in field [:points :fess])
        fess-x-rel-dexter (- (first fess) (first top-left))
        fess-y-rel-dexter (- (second fess) (second top-left))
        fess-dir-dexter [1 (/ fess-y-rel-dexter fess-x-rel-dexter)]
        bend-intersection-sinister [(* (first fess-dir-dexter) width)
                                    (* (second fess-dir-dexter) width)]
        fess-x-rel-sinister (- (first fess) (first top-right))
        fess-y-rel-sinister (- (second fess) (second top-right))
        fess-dir-sinister [1 (/ fess-y-rel-sinister fess-x-rel-sinister)]
        bend-intersection-dexter (svg/translate
                                  top-right
                                  [(* (first fess-dir-sinister) (- width))
                                   (* (second fess-dir-sinister) (- width))])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate bend-intersection-sinister [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "L" (svg/translate bend-intersection-dexter [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-chevron :top)})
        field-2 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-sinister
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "L" bend-intersection-dexter
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-chevron :bottom)}})]
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

(defn per-saltire [parts field top-level-render]
  (let [mask-id-1 (svg/id "division-saltire-1_")
        mask-id-2 (svg/id "division-saltire-2_")
        mask-id-3 (svg/id "division-saltire-3_")
        mask-id-4 (svg/id "division-saltire-4_")
        top-left (get-in field [:points :top-left])
        top-right (get-in field [:points :top-right])
        bottom-left (get-in field [:points :bottom-left])
        bottom-right (get-in field [:points :bottom-right])
        width (:width field)
        fess (get-in field [:points :fess])
        fess-x-rel-dexter (- (first fess) (first top-left))
        fess-y-rel-dexter (- (second fess) (second top-left))
        fess-dir-dexter [1 (/ fess-y-rel-dexter fess-x-rel-dexter)]
        bend-intersection-sinister [(* (first fess-dir-dexter) width)
                                    (* (second fess-dir-dexter) width)]
        fess-x-rel-sinister (- (first fess) (first top-right))
        fess-y-rel-sinister (- (second fess) (second top-right))
        fess-dir-sinister [1 (/ fess-y-rel-sinister fess-x-rel-sinister)]
        bend-intersection-dexter (svg/translate
                                  top-right
                                  [(* (first fess-dir-sinister) (- width))
                                   (* (second fess-dir-sinister) (- width))])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate top-right [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "L" (svg/translate top-left [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-saltire :top)})
        field-2 (field/make-field (svg/make-path ["M" top-right
                                                  "L" (svg/translate bend-intersection-sinister [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "L" fess
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-saltire :right)}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-sinister
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "L" (svg/translate bend-intersection-dexter [0 (- overlap)])
                                                  "L" (svg/translate fess [(- overlap) 0])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-saltire :bottom)}})
        field-4 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-dexter
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-saltire :left)}})]
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

(defn quarterly [parts field top-level-render]
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
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" (svg/translate top-right [overlap 0])
                                                  "L" (svg/translate fess [overlap overlap])
                                                  "L" (svg/translate dexter [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-quarterly :top-left)})
        field-2 (field/make-field (svg/make-path ["M" chief
                                                  "L" top-right
                                                  "L" (svg/translate sinister [0 overlap])
                                                  "L" (svg/translate fess [overlap overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-quarterly :top-right)}})
        field-3 (field/make-field (svg/make-path ["M" (svg/translate fess [(- overlap) 0])
                                                  "L" sinister
                                                  "L" bottom-right
                                                  "L" (svg/translate base [(- overlap) 0])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-quarterly :bottom-right)}})
        field-4 (field/make-field (svg/make-path ["M" fess
                                                  "L" base
                                                  "L" bottom-left
                                                  "L" dexter
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-quarterly :bottom-left)}})]
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

(defn gyronny [parts field top-level-render]
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
        width (:width field)
        fess-x-rel-dexter (- (first fess) (first top-left))
        fess-y-rel-dexter (- (second fess) (second top-left))
        fess-dir-dexter [1 (/ fess-y-rel-dexter fess-x-rel-dexter)]
        bend-intersection-sinister [(* (first fess-dir-dexter) width)
                                    (* (second fess-dir-dexter) width)]
        fess-x-rel-sinister (- (first fess) (first top-right))
        fess-y-rel-sinister (- (second fess) (second top-right))
        fess-dir-sinister [1 (/ fess-y-rel-sinister fess-x-rel-sinister)]
        bend-intersection-dexter (svg/translate
                                  top-right
                                  [(* (first fess-dir-sinister) (- width))
                                   (* (second fess-dir-sinister) (- width))])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" (svg/translate chief [overlap 0])
                                                  "L" (svg/translate fess [overlap 0])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "L" (svg/translate top-left [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-gyronny :one)})
        field-2 (field/make-field (svg/make-path ["M" fess
                                                  "L" chief
                                                  "L" top-right
                                                  "L" (svg/translate top-right [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :two)}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  "L" top-right
                                                  "L" (svg/translate sinister [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :three)}})
        field-4 (field/make-field (svg/make-path ["M" fess
                                                  "L" sinister
                                                  "L" (svg/translate bend-intersection-sinister [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :four)}})
        field-5 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-sinister
                                                  "L" bottom-right
                                                  "L" (svg/translate base [(- overlap) 0])
                                                  "L" (svg/translate fess [(- overlap) 0])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :five)}})
        field-6 (field/make-field (svg/make-path ["M" fess
                                                  "L" base
                                                  "L" bottom-left
                                                  "L" (svg/translate bend-intersection-dexter [0 (- overlap)])
                                                  "L" (svg/translate fess [(- overlap) 0])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :six)}})
        field-7 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-dexter
                                                  "L" (svg/translate dexter [0 (- overlap)])
                                                  "L" (svg/translate fess [0 (- overlap)])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :seven)}})
        field-8 (field/make-field (svg/make-path ["M" fess
                                                  "L" dexter
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-gyronny :eight)}})]
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

(defn tierced-in-pale [parts field top-level-render]
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
        col1 (- (first fess) (/ width 6))
        col2 (+ (first fess) (/ width 6))
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" (svg/translate [col1 (second chief)] [overlap 0])
                                                  "L" (svg/translate [col1 (second base)] [overlap 0])
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-tierced-pale :left)})
        field-2 (field/make-field (svg/make-path ["M" [col1 (second chief)]
                                                  "L" (svg/translate [col2 (second chief)] [overlap 0])
                                                  "L" (svg/translate [col2 (second base)] [overlap 0])
                                                  "L" [col1 (second base)]
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pale :middle)}})
        field-3 (field/make-field (svg/make-path ["M" [col2 (second chief)]
                                                  "L" top-right
                                                  "L" bottom-right
                                                  "L" [col2 (second base)]
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pale :right)}})]
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

(defn tierced-in-fesse [parts field top-level-render]
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
        row1 (- (second fess) (/ height 6))
        row2 (+ (second fess) (/ height 6))
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate [(first sinister) row1] [0 overlap])
                                                  "L" (svg/translate [(first dexter) row1] [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-tierced-fesse :top)})
        field-2 (field/make-field (svg/make-path ["M" [(first dexter) row1]
                                                  "L" [(first sinister) row1]
                                                  "L" (svg/translate [(first sinister) row2] [0 overlap])
                                                  "L" (svg/translate [(first dexter) row2] [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-fesse :middle)}})
        field-3 (field/make-field (svg/make-path ["M" [(first dexter) row2]
                                                  "L" [(first sinister) row2]
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-fesse :bottom)}})]
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
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" top-right
                                                  "L" (svg/translate top-right [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "L" (svg/translate top-left [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-tierced-pairle :top)})
        field-2 (field/make-field (svg/make-path ["M" fess
                                                  "L" top-right
                                                  "L" bottom-right
                                                  "L" (svg/translate base [(- overlap) 0])
                                                  "L" (svg/translate fess [(- overlap) 0])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pairle :right)}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  "L" base
                                                  "L" bottom-left
                                                  "L" top-left
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pall :left)}})]
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
        fess-x-rel-dexter (- (first fess) (first top-left))
        fess-y-rel-dexter (- (second fess) (second top-left))
        fess-dir-dexter [1 (/ fess-y-rel-dexter fess-x-rel-dexter)]
        bend-intersection-sinister [(* (first fess-dir-dexter) width)
                                    (* (second fess-dir-dexter) width)]
        fess-x-rel-sinister (- (first fess) (first top-right))
        fess-y-rel-sinister (- (second fess) (second top-right))
        fess-dir-sinister [1 (/ fess-y-rel-sinister fess-x-rel-sinister)]
        bend-intersection-dexter (svg/translate
                                  top-right
                                  [(* (first fess-dir-sinister) (- width))
                                   (* (second fess-dir-sinister) (- width))])
        parent-meta (:meta field)
        field-1 (field/make-field (svg/make-path ["M" top-left
                                                  "L" (svg/translate chief [overlap 0])
                                                  "L" (svg/translate fess [overlap 0])
                                                  "L" (svg/translate bend-intersection-dexter [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :context (conj (:context parent-meta) :per-tierced-pairle-reversed :left)})
        field-2 (field/make-field (svg/make-path ["M" chief
                                                  "L" top-right
                                                  "L" (svg/translate bend-intersection-sinister [0 overlap])
                                                  "L" (svg/translate fess [0 overlap])
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pairle-reversed :right)}})
        field-3 (field/make-field (svg/make-path ["M" fess
                                                  "L" bend-intersection-sinister
                                                  "L" bottom-right
                                                  "L" bottom-left
                                                  "L" bend-intersection-dexter
                                                  "z"])
                                  {:parent field
                                   :meta {:context (conj (:context parent-meta) :per-tierced-pall-reversed :bottom)}})]
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

;; TODO: offset and/or number of stripes to fit, which dictates their width


(defn paly [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -37.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -12.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 12.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 37.5,-1000 h 12.5 v 2000 h -12.5 z"
           :fill (get tinctures stripe)}]])

(defn barry [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -1000,-37.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-12.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,12.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,37.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,62.5 v 12.5 h 2000 v -12.5 z"
           :fill (get tinctures stripe)}]])

(defn bendy [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m -1000,-1000 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-1050 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-1100 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-950 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m -1000,-900 v 25 l 2000,2000 v -25 z"
           :fill (get tinctures stripe)}]])

(defn bendy-sinister [[base stripe]]
  [:<>
   [base-area (get tinctures base)]
   [:path {:d "m 1000,-1025 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-1075 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-1125 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-975 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]
   [:path {:d "m 1000,-925 v 25 l -2000,2000 v -25 z"
           :fill (get tinctures stripe)}]])

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
   ["Tierced in Pairle Reversed" :tierced-in-pairle-reversed tierced-in-pairle-reversed]
   ["Paly" :paly paly]
   ["Barry" :barry barry]
   ["Bendy" :bendy bendy]
   ["Bendy Sinister" :bendy-sinister bendy-sinister]])

(def kinds-function-map
  (->> kinds
       (map (fn [[_ key function]]
              [key function]))
       (into {})))

(def options
  (->> kinds
       (map (fn [[name key _]]
              [key name]))))

(defn render [{:keys [type parts]} field top-level-render]
  (let [function (get kinds-function-map type)]
    [function parts field top-level-render]))
