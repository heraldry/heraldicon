(ns heraldry.coat-of-arms.charge.core
  (:require ["svgpath" :as svgpath]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.escutcheon :as escutcheon]
            [heraldry.coat-of-arms.field-environment :as field-environment]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn remove-outlines [data placeholder-colours]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill}))
                           (->> % second svg/normalize-colour (get placeholder-colours) (= :outline)))
                    [(first %) "none"]
                    %)
                 data))

(defn replace-colours [data function]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill}))
                           (-> % second (not= "none")))
                    [(first %) (function (second %))]
                    %)
                 data))

(defn get-replacement [kind provided-placeholder-colours]
  (let [replacement (get provided-placeholder-colours kind)]
    (if (or (nil? replacement)
            (= replacement :none))
      nil
      replacement)))

(defn make-mask [data placeholder-colours provided-placeholder-colours outline-mode]
  (let [mask-id          (util/id "mask")
        mask-inverted-id (util/id "mask")
        mask             (replace-colours
                          data
                          (fn [colour]
                            (let [colour-lower (svg/normalize-colour colour)
                                  kind         (get placeholder-colours colour-lower)
                                  replacement  (get-replacement kind provided-placeholder-colours)]
                              (if (or (= kind :keep)
                                      (and (not (#{:transparent :primary} outline-mode))
                                           (= kind :outline))
                                      replacement)
                                "#fff"
                                "#000"))))
        mask-inverted    (replace-colours
                          data
                          (fn [colour]
                            (let [colour-lower (svg/normalize-colour colour)
                                  kind         (get placeholder-colours colour-lower)
                                  replacement  (get-replacement kind provided-placeholder-colours)]
                              (if (or (= kind :keep)
                                      (and (not (#{:primary} outline-mode))
                                           (= kind :outline))
                                      replacement)
                                "#000"
                                "#fff"))))]
    [mask-id mask mask-inverted-id mask-inverted]))

(defn make-charge
  [{:keys [field hints] :as charge} parent environment {:keys [render-options] :as context} arg function]
  (let [{:keys [position geometry]}   (options/sanitize charge (charge-options/options charge))
        {:keys [size stretch rotation
                mirrored? reversed?]} geometry
        position-point                (position/calculate position environment :fess)
        arg-value                     (get environment arg)
        target-arg-value              (-> size
                                          ((util/percent-of arg-value)))
        scale-x                       (if mirrored? -1 1)
        scale-y                       (* (if reversed? -1 1) stretch)
        {:keys [shape
                mask
                charge-width
                charge-height]}       (function target-arg-value)
        charge-shape                  (-> shape
                                          svg/make-path
                                          (->
                                           (svgpath)
                                           (.scale scale-x scale-y)
                                           (.toString))
                                          (cond->
                                              (:squiggly? render-options) svg/squiggly-path
                                              (not= rotation 0) (->
                                                                 (svgpath)
                                                                 (.rotate rotation)
                                                                 (.toString)))
                                          (svg/translate (:x position-point) (:y position-point)))
        mask-shape                    (when mask
                                        (-> mask
                                            svg/make-path
                                            (->
                                             (svgpath)
                                             (.scale scale-x scale-y)
                                             (.toString))
                                            (cond->
                                                (:squiggly? render-options) svg/squiggly-path
                                                (not= rotation 0) (->
                                                                   (svgpath)
                                                                   (.rotate rotation)
                                                                   (.toString)))
                                            (svg/translate (:x position-point) (:y position-point))))
        [min-x max-x min-y max-y]     (svg/rotated-bounding-box (v//
                                                                 (v/v charge-width
                                                                      charge-height)
                                                                 -2)
                                                                (v//
                                                                 (v/v charge-width
                                                                      charge-height)
                                                                 2)
                                                                rotation
                                                                :scale (v/v scale-x scale-y))
        box-size                      (v/v (- max-x min-x)
                                           (- max-y min-y))
        parts                         [[charge-shape
                                        [(v/- position-point
                                              (v// box-size 2))
                                         (v/+ position-point
                                              (v// box-size 2))]
                                        mask-shape]]
        field                         (if (counterchange/counterchangable? field parent)
                                        (counterchange/counterchange-field field parent)
                                        field)]
    [division-shared/make-division
     :charge-pale [field] parts
     [:all]
     (when (or (:outline? render-options)
               (-> hints :outline-mode (= :keep)))
       [:g outline/style
        [:path {:d charge-shape}]
        (when mask-shape
          [:path {:d mask-shape}])])
     environment charge context]))

(defn escutcheon
  {:display-name "Escutcheon"}
  [charge parent environment {:keys [root-escutcheon] :as context}]
  (let [{:keys [escutcheon]} (options/sanitize charge (charge-options/options charge))]
    (make-charge charge parent environment context
                 :width
                 (fn [width]
                   (let [env      (field-environment/transform-to-width
                                   (escutcheon/field (if (= escutcheon :none)
                                                       root-escutcheon
                                                       escutcheon)) width)
                         env-fess (-> env :points :fess)]
                     {:shape         (svg/translate (:shape env)
                                                    (-> env-fess :x -)
                                                    (-> env-fess :y -))
                      :charge-width  width
                      :charge-height width})))))

(defn roundel
  {:display-name "Roundel"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius (/ width 2)]
                   {:shape         ["m" (v/v radius 0)
                                    ["a" radius radius
                                     0 0 0 (v/v (- width) 0)]
                                    ["a" radius radius
                                     0 0 0 width 0]
                                    "z"]
                    :charge-width  width
                    :charge-height width}))))

(defn annulet
  {:display-name "Annulet"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius      (/ width 2)
                       hole-radius (* radius 0.6)]
                   {:shape         ["m" (v/v radius 0)
                                    ["a" radius radius
                                     0 0 0 (v/v (- width) 0)]
                                    ["a" radius radius
                                     0 0 0 width 0]
                                    "z"]
                    :mask          ["m" (v/v hole-radius 0)
                                    ["a" hole-radius hole-radius
                                     0 0 0 (v/v (* hole-radius -2) 0)]
                                    ["a" hole-radius hole-radius
                                     0 0 0 (* hole-radius 2) 0]
                                    "z"]
                    :charge-width  width
                    :charge-height width}))))

(defn billet
  {:display-name "Billet"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width       (/ height 2)
                       width-half  (/ width 2)
                       height-half (/ height 2)]
                   {:shape         ["m" (v/v (- width-half) (- height-half))
                                    "h" width
                                    "v" height
                                    "h" (- width)
                                    "z"]
                    :charge-width  width
                    :charge-height height}))))

(defn lozenge
  {:display-name "Lozenge"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width       (/ height 1.3)
                       width-half  (/ width 2)
                       height-half (/ height 2)]
                   {:shape         ["m" (v/v 0 (- height-half))
                                    "l" (v/v width-half height-half)
                                    "l " (v/v (- width-half) height-half)
                                    "l" (v/v (- width-half) (- height-half))
                                    "z"]
                    :charge-width  width
                    :charge-height height}))))

(defn fusil
  {:display-name "Fusil"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width       (/ height 2)
                       width-half  (/ width 2)
                       height-half (/ height 2)]
                   {:shape         ["m" (v/v 0 (- height-half))
                                    "l" (v/v width-half height-half)
                                    "l " (v/v (- width-half) height-half)
                                    "l" (v/v (- width-half) (- height-half))
                                    "z"]
                    :charge-width  width
                    :charge-height height}))))

(defn mascle
  {:display-name "Mascle"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width            (/ height 1.3)
                       width-half       (/ width 2)
                       height-half      (/ height 2)
                       hole-width       (* width 0.55)
                       hole-height      (* height 0.55)
                       hole-width-half  (/ hole-width 2)
                       hole-height-half (/ hole-height 2)]
                   {:shape         ["m" (v/v 0 (- height-half))
                                    "l" (v/v width-half height-half)
                                    "l " (v/v (- width-half) height-half)
                                    "l" (v/v (- width-half) (- height-half))
                                    "z"]
                    :mask          ["m" (v/v 0 (- hole-height-half))
                                    "l" (v/v hole-width-half hole-height-half)
                                    "l " (v/v (- hole-width-half) hole-height-half)
                                    "l" (v/v (- hole-width-half) (- hole-height-half))
                                    "z"]
                    :charge-width  width
                    :charge-height height}))))

(defn rustre
  {:display-name "Rustre"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :height
               (fn [height]
                 (let [width       (/ height 1.3)
                       width-half  (/ width 2)
                       height-half (/ height 2)
                       hole-radius (/ width 4)]
                   {:shape         ["m" (v/v 0 (- height-half))
                                    "l" (v/v width-half height-half)
                                    "l " (v/v (- width-half) height-half)
                                    "l" (v/v (- width-half) (- height-half))
                                    "z"]
                    :mask          ["m" (v/v hole-radius 0)
                                    ["a" hole-radius hole-radius
                                     0 0 0 (v/v (* hole-radius -2) 0)]
                                    ["a" hole-radius hole-radius
                                     0 0 0 (* hole-radius 2) 0]
                                    "z"]
                    :charge-width  width
                    :charge-height height}))))

(defn crescent
  {:display-name "Crescent"}
  [charge parent environment context]
  (make-charge charge parent environment context
               :width
               (fn [width]
                 (let [radius       (/ width 2)
                       inner-radius (* radius
                                       0.75)
                       horn-angle   -45
                       horn-point-x (* radius
                                       (-> horn-angle
                                           (* Math/PI)
                                           (/ 180)
                                           Math/cos))
                       horn-point-y (* radius
                                       (-> horn-angle
                                           (* Math/PI)
                                           (/ 180)
                                           Math/sin))
                       horn-point-1 (v/v horn-point-x horn-point-y)
                       horn-point-2 (v/v (- horn-point-x) horn-point-y)]
                   {:shape         ["m" horn-point-1
                                    ["a" radius radius
                                     0 1 1 (v/- horn-point-2 horn-point-1)]
                                    ["a" inner-radius inner-radius
                                     0 1 0 (v/- horn-point-1 horn-point-2)]
                                    "z"]
                    :charge-width  width
                    :charge-height width}))))

(def charges
  [#'roundel
   #'annulet
   #'billet
   #'escutcheon
   #'lozenge
   #'fusil
   #'mascle
   #'rustre
   #'crescent])

(def kinds-function-map
  (->> charges
       (map (fn [function]
              [(-> function meta :name keyword) function]))
       (into {})))

(def choices
  (->> charges
       (map (fn [function]
              [(-> function meta :display-name) (-> function meta :name keyword)]))))

(defn render-other-charge [{:keys [type
                                   field
                                   tincture
                                   hints
                                   variant
                                   data]
                            :as   charge}
                           parent
                           environment
                           {:keys [render-field
                                   render-options
                                   load-charge-data
                                   fn-select-component
                                   svg-export?]
                            :as   context}]
  (if-let [full-charge-data (or data (load-charge-data variant))]
    (let [{:keys [position geometry]}      (options/sanitize charge (charge-options/options charge))
          {:keys [size stretch
                  mirrored? reversed?
                  rotation]}               geometry
          charge-data                      (:data full-charge-data)
          render-field?                    (-> charge-data
                                               :fixed-tincture
                                               (or :none)
                                               (= :none))
          ;; since size now is filled with a default, check whether it was set at all,
          ;; if not, then use nil
          ;; TODO: this probably needs a better mechanism and form representation
          size                             (if (-> charge :geometry :size) size nil)
          points                           (:points environment)
          top                              (:top points)
          bottom                           (:bottom points)
          left                             (:left points)
          right                            (:right points)
          positional-charge-width          (js/parseFloat (-> charge-data :width (or "1")))
          positional-charge-height         (js/parseFloat (-> charge-data :height (or "1")))
          width                            (:width environment)
          height                           (:height environment)
          center-point                     (position/calculate position environment :fess)
          min-x-distance                   (min (- (:x center-point) (:x left))
                                                (- (:x right) (:x center-point)))
          min-y-distance                   (min (- (:y center-point) (:y top))
                                                (- (:y bottom) (:y center-point)))
          target-width                     (if size
                                             (-> size
                                                 ((util/percent-of width)))
                                             (* (* min-x-distance 2) 0.8))
          target-height                    (/ (if size
                                                (-> size
                                                    ((util/percent-of height)))
                                                (* (* min-y-distance 2) 0.7))
                                              stretch)
          scale-x                          (* (if mirrored? -1 1)
                                              (min (/ target-width positional-charge-width)
                                                   (/ target-height positional-charge-height)))
          scale-y                          (* (if reversed? -1 1)
                                              (* (Math/abs scale-x) stretch))
          placeholder-colours              (-> full-charge-data
                                               :colours
                                               (cond->>
                                                   (:preview-original? render-options)
                                                 (into {}
                                                       (map (fn [[k _]]
                                                              [k :keep])))))
          adjusted-charge                  (-> charge-data
                                               :data
                                               svg/fix-string-style-values
                                               (cond->
                                                   (not (or (-> hints :outline-mode (not= :remove))
                                                            (:outline? render-options))) (remove-outlines placeholder-colours)))
          [mask-id mask
           mask-inverted-id mask-inverted] (make-mask adjusted-charge
                                                      placeholder-colours
                                                      tincture
                                                      (:outline-mode hints))
          ;; this is the one drawn on top of the masked field version, for the tincture replacements
          ;; and outline; the primary colour has no replacement here, which might make it shine through
          ;; around the edges, to prevent that, it is specifically replaced with black
          ;; the same is true for tincture codes for which no replacement has been set, as those should
          ;; become primary
          ;; so only render things that have a replacement or explicitly are set to "keep",
          ;; other things become black for the time being
          ;; TODO: perhaps they can be removed entirely? there still is a faint dark edge in some cases,
          ;; much less than before, however, and the dark edge is less obvious than the bright one
          coloured-charge                  (replace-colours
                                            adjusted-charge
                                            (fn [colour]
                                              (let [colour-lower (svg/normalize-colour colour)
                                                    kind         (get placeholder-colours colour-lower)
                                                    replacement  (get-replacement kind tincture)]
                                                (cond
                                                  replacement    (tincture/pick replacement render-options)
                                                  (= kind :keep) colour
                                                  :else          "#000000"))))
          clip-path-id                     (util/id "clip-path")
          shift                            (-> (v/v positional-charge-width positional-charge-height)
                                               (v// 2)
                                               (v/-))
          [min-x max-x min-y max-y]        (svg/rotated-bounding-box
                                            shift
                                            (v/dot shift (v/v -1 -1))
                                            rotation
                                            :scale (v/v scale-x scale-y))
          clip-size                        (v/v (- max-x min-x) (- max-y min-y))
          position                         (-> clip-size
                                               (v/-)
                                               (v// 2)
                                               (v/+ center-point))
          charge-environment               (field-environment/create
                                            (svg/make-path ["M" position
                                                            "l" (v/v (:x clip-size) 0)
                                                            "l" (v/v 0 (:y clip-size))
                                                            "l" (v/v (- (:x clip-size)) 0)
                                                            "l" (v/v 0 (- (:y clip-size)))
                                                            "z"])
                                            {:parent               field
                                             :context              [:charge]
                                             :bounding-box         (svg/bounding-box
                                                                    [position (v/+ position
                                                                                   clip-size)])
                                             :override-environment (when (or (:inherit-environment? field)
                                                                             (counterchange/counterchangable? field parent)) environment)})
          field                            (if (counterchange/counterchangable? field parent)
                                             (counterchange/counterchange-field field parent)
                                             field)
          charge-name                      (or (:name full-charge-data) "")
          username                         (:username full-charge-data)
          charge-url                       (or (util/full-url-for-charge full-charge-data) "")
          attribution                      (:attribution full-charge-data)]
      [:<>
       [:defs
        [:mask {:id mask-id}
         mask]
        [:mask {:id mask-inverted-id}
         mask-inverted]
        (when-not svg-export?
          [:clipPath {:id clip-path-id}
           [:rect {:x      0
                   :y      0
                   :width  positional-charge-width
                   :height positional-charge-height
                   :fill   "#fff"}]])]
       (let [transform         (str "translate(" (:x center-point) "," (:y center-point) ")"
                                    "rotate(" rotation ")"
                                    "scale(" scale-x "," scale-y ")"
                                    "translate(" (-> shift :x) "," (-> shift :y) ")")
             reverse-transform (str "translate(" (-> shift :x -) "," (-> shift :y -) ")"
                                    "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                    "rotate(" (- rotation) ")"
                                    "translate(" (- (:x center-point)) "," (- (:y center-point)) ")")]
         [:g {:transform transform
              :clip-path (when-not svg-export?
                           (str "url(#" clip-path-id ")"))}
          [metadata/attribution charge-name username (util/full-url-for-username username) charge-url attribution]
          (when render-field?
            [:g {:mask (str "url(#" mask-inverted-id ")")}
             [:g {:transform reverse-transform}
              [render-field field charge-environment (-> context
                                                         (update :db-path conj :field)
                                                         (dissoc :fn-select-component))]]])
          [:g {:mask     (str "url(#" mask-id ")")
               :on-click (when fn-select-component
                           (fn [event]
                             (fn-select-component (-> context
                                                      :db-path
                                                      (conj :field)))
                             (.stopPropagation event)))}
           coloured-charge]])])
    [:<>]))

(defn render [{:keys [type variant data] :as charge} parent environment context]
  (let [function (get kinds-function-map type)]
    (if (and function
             (not data)
             (not variant))
      [function charge parent environment context]
      [render-other-charge charge parent environment context])))
