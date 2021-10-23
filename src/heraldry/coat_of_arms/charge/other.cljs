(ns heraldry.coat-of-arms.charge.other
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.charge.interface :as charge-interface]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.metadata :as metadata]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.colour :as colour]
            [heraldry.interface :as interface]
            [heraldry.math.bounding-box :as bounding-box]
            [heraldry.math.svg.core :as svg]
            [heraldry.math.svg.path :as path]
            [heraldry.math.vector :as v]
            [heraldry.util :as util]))

(defn placeholder-colour-modifier [placeholder-colours colour]
  (let [value (get placeholder-colours colour)]
    (if (vector? value)
      (first value)
      value)))

(defn placeholder-colour-qualifier [placeholder-colours colour]
  (let [value (get placeholder-colours colour)]
    (if (vector? value)
      (second value)
      :none)))

(defn -remove-outlines [data placeholder-colours]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (or (-> % second (s/starts-with? "url"))
                               (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) (= :outline))))
                    [(first %) "none"]
                    %)
                 data))

(def remove-outlines (memoize -remove-outlines))

(defn -remove-shading [data placeholder-colours]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (or (-> % second (s/starts-with? "url"))
                               (->> % second colour/normalize (placeholder-colour-modifier placeholder-colours) #{:shadow :highlight})))
                    [(first %) "none"]
                    %)
                 data))

(def remove-shading (memoize -remove-shading))

(defn -replace-colours [data function]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (-> % second (not= "none")))
                    [(first %) (function (second %))]
                    %)
                 data))

(def replace-colours (memoize -replace-colours))

(defn -set-layer-separator-opacity [data layer-separator-colours opacity]
  (if (seq layer-separator-colours)
    (let [layer-separator-colours (set layer-separator-colours)]
      (walk/postwalk (fn [v]
                       (if (map? v)
                         (loop [v v
                                [[attribute opacity-attribute] & rest] [[:stroke :stroke-opacity]
                                                                        [:fill :fill-opacity]
                                                                        [:stop-color :stop-opacity]]]
                           (let [new-v (cond-> v
                                         (some-> (get v attribute)
                                                 colour/normalize
                                                 layer-separator-colours) (-> (assoc opacity-attribute opacity)
                                                                              (cond->
                                                                                (:opacity v) (assoc :opacity opacity)))
                                         )]
                             (if (seq rest)
                               (recur new-v rest)
                               new-v)))
                         v))
                     data))
    data))

(def set-layer-separator-opacity (memoize -set-layer-separator-opacity))

(defn get-replacement [kind provided-placeholder-colours]
  (let [replacement (get provided-placeholder-colours kind)]
    (if (or (nil? replacement)
            (= replacement :none))
      nil
      replacement)))

(defn -make-mask [data placeholder-colours provided-placeholder-colours
                  outline-mode preview-original? hide-lower-layer?]
  (let [mask-id (util/id "mask")
        mask-inverted-id (util/id "mask")
        mask (replace-colours
              data
              (fn [colour]
                (if (s/starts-with? colour "url")
                  "none"
                  (let [colour-lower (colour/normalize colour)
                        kind (placeholder-colour-modifier placeholder-colours colour-lower)
                        replacement (get-replacement kind provided-placeholder-colours)]
                    (cond
                      (or preview-original?
                          (= kind :keep)
                          (and (not (#{:transparent :primary} outline-mode))
                               (= kind :outline))
                          replacement) "#fff"
                      (and (= kind :layer-separator)
                           (not hide-lower-layer?)) "none"
                      (and (= kind :layer-separator)
                           hide-lower-layer?) "#000"
                      :else "#000")))))
        mask-inverted (replace-colours
                       data
                       (fn [colour]
                         (if (s/starts-with? colour "url")
                           "none"
                           (let [colour-lower (colour/normalize colour)
                                 kind (placeholder-colour-modifier placeholder-colours colour-lower)
                                 replacement (get-replacement kind provided-placeholder-colours)]
                             (cond
                               (or preview-original?
                                   (= kind :keep)
                                   (and (not (#{:primary} outline-mode))
                                        (= kind :outline))
                                   replacement) "#000"
                               (and (= kind :layer-separator)
                                    (not hide-lower-layer?)) "none"
                               (and (= kind :layer-separator)
                                    hide-lower-layer?) "#000"
                               :else "#fff")))))]
    [mask-id mask mask-inverted-id mask-inverted]))

(def make-mask (memoize -make-mask))

(defmethod charge-interface/render-charge :heraldry.charge.type/other
  [path _parent-path environment {:keys [load-charge-data charge-group
                                         origin-override size-default
                                         self-below-shield? render-pass-below-shield?
                                         auto-resize?] :as context
                                  :or {auto-resize? true}}]
  (let [data (interface/get-raw-data (conj path :data) context)
        variant (interface/get-raw-data (conj path :variant) context)
        full-charge-data (or data (when variant (load-charge-data variant)))
        placeholder-colours (:colours full-charge-data)
        layer-separator-colours (->> placeholder-colours
                                     (keep (fn [[colour placeholder]]
                                             (when (= placeholder :layer-separator)
                                               (colour/normalize colour))))
                                     set)
        ignore-layer-separator? (interface/get-sanitized-data (conj path :ignore-layer-separator?) context)]
    (if (and (:data full-charge-data)
             ;; in order to require rendering, we either have
             ;; to be located in the right render pass
             ;; OR have some layer separator in the charge
             (or (= (boolean self-below-shield?)
                    (boolean render-pass-below-shield?))
                 (and (not ignore-layer-separator?)
                      (seq layer-separator-colours))))
      (let [context (-> context
                        (dissoc :origin-override)
                        (dissoc :size-default)
                        (dissoc :charge-group))
            origin (interface/get-sanitized-data (conj path :origin) context)
            anchor (interface/get-sanitized-data (conj path :anchor) context)
            vertical-mask (interface/get-sanitized-data (conj path :vertical-mask) context)
            fimbriation (interface/get-sanitized-data (conj path :fimbriation) context)
            size (if (and size-default
                          (not (interface/get-raw-data (conj path :geometry :size) context)))
                   size-default
                   (interface/get-sanitized-data (conj path :geometry :size) context))
            stretch (interface/get-sanitized-data (conj path :geometry :stretch) context)
            mirrored? (interface/get-sanitized-data (conj path :geometry :mirrored?) context)
            reversed? (interface/get-sanitized-data (conj path :geometry :reversed?) context)
            ;; not all tinctures have their own options, but some do, so
            ;; override those with the sanitized values
            tincture (merge
                      (interface/get-raw-data (conj path :tincture) context)
                      (interface/get-sanitized-data (conj path :tincture) context))
            preview-original? (interface/render-option :preview-original? context)
            outline-mode (if (or (interface/render-option :outline? context)
                                 (= (interface/render-option :mode context)
                                    :hatching)) :keep
                             (interface/get-sanitized-data (conj path :outline-mode) context))
            outline? (= outline-mode :keep)
            {:keys [slot-spacing
                    slot-angle]} charge-group
            context (dissoc context :charge-group)
            charge-data (:data full-charge-data)
            render-field? (-> charge-data
                              :fixed-tincture
                              (or :none)
                              (= :none))
            ;; since size now is filled with a default, check whether it was set at all,
            ;; if not, then use nil; exception: if auto-resize? is false, then always use
            ;; the sanitized value
            ;; TODO: this probably needs a better mechanism and form representation
            size (if (or (not auto-resize?)
                         (interface/get-raw-data (conj path :geometry :size) context))
                   size
                   nil)
            points (:points environment)
            top (:top points)
            bottom (:bottom points)
            left (:left points)
            right (:right points)
            positional-charge-width (js/parseFloat (-> charge-data :width (or "1")))
            positional-charge-height (js/parseFloat (-> charge-data :height (or "1")))
            width (:width environment)
            height (:height environment)
            environment-for-origin (if origin-override
                                     (assoc-in environment [:points :special] origin-override)
                                     environment)
            origin (if origin-override
                     {:point :special
                      :offset-x 0
                      :offset-y 0}
                     origin)
            {origin-point :real-origin
             anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                         environment-for-origin
                                         origin
                                         anchor
                                         0
                                         -90)
            angle (+ (v/angle-to-point origin-point anchor-point)
                     90)
            min-x-distance (or (some-> slot-spacing :width (/ 2) (/ 0.9))
                               (min (- (:x origin-point) (:x left))
                                    (- (:x right) (:x origin-point))))
            min-y-distance (or (some-> slot-spacing :height (/ 2) (/ 0.8))
                               (min (- (:y origin-point) (:y top))
                                    (- (:y bottom) (:y origin-point))))
            target-width (if size
                           (-> size
                               ((util/percent-of width)))
                           (* (* min-x-distance 2) 0.8))
            target-height (/ (if size
                               (-> size
                                   ((util/percent-of height)))
                               (* (* min-y-distance 2) 0.7))
                             stretch)
            angle (if (and (-> anchor :point (= :angle))
                           slot-angle)
                    (if (< positional-charge-height positional-charge-width)
                      (+ angle slot-angle 90)
                      (+ angle slot-angle))
                    angle)
            scale-x (* (if mirrored? -1 1)
                       (min (/ target-width positional-charge-width)
                            (/ target-height positional-charge-height)))
            scale-y (* (if reversed? -1 1)
                       (* (Math/abs scale-x) stretch))
            render-shadow? (and (-> placeholder-colours
                                    vals
                                    set
                                    (get :shadow))
                                (:shadow tincture))
            shadow-mask-id (when render-shadow?
                             (util/id "mask"))
            shadow-helper-mask-id (when render-shadow?
                                    (util/id "mask"))
            render-highlight? (and (-> placeholder-colours
                                       vals
                                       set
                                       (get :highlight))
                                   (:highlight tincture))
            highlight-mask-id (when render-highlight?
                                (util/id "mask"))
            highlight-helper-mask-id (when render-highlight?
                                       (util/id "mask"))
            unadjusted-charge (:data charge-data)
            adjusted-charge (-> unadjusted-charge
                                (cond->
                                  (not preview-original?) (set-layer-separator-opacity
                                                           layer-separator-colours
                                                           1)
                                  (= outline-mode :remove) (remove-outlines placeholder-colours)))
            adjusted-charge-without-shading (-> adjusted-charge
                                                (remove-shading placeholder-colours))
            hide-lower-layer? (and (seq layer-separator-colours)
                                   (not ignore-layer-separator?)
                                   (not render-pass-below-shield?))
            [mask-id mask
             mask-inverted-id mask-inverted] (make-mask adjusted-charge-without-shading
                                                        placeholder-colours
                                                        tincture
                                                        outline-mode
                                                        preview-original?
                                                        hide-lower-layer?)
            ;; this is the one drawn on top of the masked field version, for the tincture replacements
            ;; and outline; the primary colour has no replacement here, which might make it shine through
            ;; around the edges, to prevent that, it is specifically replaced with black
            ;; the same is true for tincture codes for which no replacement has been set, as those should
            ;; become primary
            ;; so only render things that have a replacement or explicitly are set to "keep",
            ;; other things become black for the time being
            ;; TODO: perhaps they can be removed entirely? there still is a faint dark edge in some cases,
            ;; much less than before, however, and the dark edge is less obvious than the bright one
            adjusted-charge-without-shading (set-layer-separator-opacity
                                             adjusted-charge-without-shading
                                             layer-separator-colours
                                             0)
            coloured-charge (replace-colours
                             adjusted-charge-without-shading
                             (fn [colour]
                               (let [colour-lower (colour/normalize colour)
                                     kind (placeholder-colour-modifier placeholder-colours colour-lower)
                                     replacement (get-replacement kind tincture)]
                                 (cond
                                   replacement (tincture/pick replacement context)
                                   (= kind :keep) colour
                                   :else (outline/color context)))))
            shift (-> (v/v positional-charge-width positional-charge-height)
                      (v/div 2)
                      (v/sub))
            [min-x max-x min-y max-y] (bounding-box/rotate
                                       shift
                                       (v/dot shift (v/v -1 -1))
                                       angle
                                       :scale (v/v scale-x scale-y))
            clip-size (v/v (- max-x min-x) (- max-y min-y))
            position (-> clip-size
                         (v/sub)
                         (v/div 2)
                         (v/add origin-point))
            inherit-environment? (interface/get-sanitized-data
                                  (conj path :field :inherit-environment?)
                                  context)
            counterchanged? (interface/get-sanitized-data
                             (conj path :field :counterchanged?)
                             context)
            charge-environment (environment/create
                                (path/make-path ["M" position
                                                 "l" (v/v (:x clip-size) 0)
                                                 "l" (v/v 0 (:y clip-size))
                                                 "l" (v/v (- (:x clip-size)) 0)
                                                 "l" (v/v 0 (- (:y clip-size)))
                                                 "z"])
                                {:parent path
                                 :parent-environment environment
                                 :context [:charge]
                                 :bounding-box (bounding-box/bounding-box
                                                [position (v/add position
                                                                 clip-size)])
                                 :override-environment (when (or inherit-environment?
                                                                 counterchanged?)
                                                         environment)})
            vertical-mask? (not (zero? vertical-mask))
            vertical-mask-id (util/id "mask")
            layer-separator-colour-for-shadow-highlight (if hide-lower-layer?
                                                          "#000000"
                                                          "none")]
        [:<>
         (when vertical-mask?
           (let [total-width (- max-x min-x)
                 total-height (- max-y min-y)
                 mask-height ((util/percent-of total-height) (Math/abs vertical-mask))]
             [:defs
              [:mask {:id vertical-mask-id}
               [:g {:transform (str "translate(" (v/->str origin-point) ")")}
                [:rect {:x (- min-x 10)
                        :y (- min-y 10)
                        :width (+ total-width 20)
                        :height (+ total-height 10)
                        :style {:fill "#ffffff"}}]
                [:rect {:x (- min-x 10)
                        :y (if (pos? vertical-mask)
                             (-> min-y
                                 (+ total-height)
                                 (- mask-height))
                             (-> min-y
                                 (- 10)))
                        :width (+ total-width 20)
                        :height (+ mask-height 10)
                        :style {:fill "#000000"}}]]]]))
         [:g (when vertical-mask?
               {:mask (str "url(#" vertical-mask-id ")")})
          [:defs
           (when render-shadow?
             [:mask {:id shadow-mask-id}
              [:defs
               [:mask {:id shadow-helper-mask-id}
                (->
                 adjusted-charge-without-shading
                 (replace-colours
                  (fn [colour]
                    (if (s/starts-with? colour "url")
                      "none"
                      (let [colour-lower (colour/normalize colour)
                            kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                        (case kind
                          :outline "#000000"
                          :shadow "none"
                          :highlight "none"
                          :layer-separator layer-separator-colour-for-shadow-highlight
                          "#ffffff")))))
                 svg/make-unique-ids)]]
              [:g {:mask (str "url(#" shadow-helper-mask-id ")")}
               (->
                adjusted-charge
                (replace-colours
                 (fn [colour]
                   (if (s/starts-with? colour "url")
                     colour
                     (let [colour-lower (colour/normalize colour)
                           kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                       (case kind
                         :shadow "#ffffff"
                         :highlight "none"
                         :layer-separator layer-separator-colour-for-shadow-highlight
                         "#000000")))))
                svg/make-unique-ids)]])
           (when render-highlight?
             [:mask {:id highlight-mask-id}
              [:defs
               [:mask {:id highlight-helper-mask-id}
                (->
                 adjusted-charge-without-shading
                 (replace-colours
                  (fn [colour]
                    (if (s/starts-with? colour "url")
                      "none"
                      (let [colour-lower (colour/normalize colour)
                            kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                        (case kind
                          :outline "#000000"
                          :shadow "none"
                          :highlight "none"
                          :layer-separator layer-separator-colour-for-shadow-highlight
                          "#ffffff")))))
                 svg/make-unique-ids)]]
              [:g {:mask (str "url(#" highlight-helper-mask-id ")")}
               (->
                adjusted-charge
                (replace-colours
                 (fn [colour]
                   (if (s/starts-with? colour "url")
                     colour
                     (let [colour-lower (colour/normalize colour)
                           kind (placeholder-colour-modifier placeholder-colours colour-lower)]
                       (case kind
                         :shadow "none"
                         :highlight "#ffffff"
                         :layer-separator layer-separator-colour-for-shadow-highlight
                         "#000000")))))
                svg/make-unique-ids)]])
           [:mask {:id mask-id}
            (svg/make-unique-ids mask)]
           [:mask {:id mask-inverted-id}
            (svg/make-unique-ids mask-inverted)]]
          (let [transform (str "translate(" (v/->str origin-point) ")"
                               "rotate(" angle ")"
                               "scale(" scale-x "," scale-y ")"
                               "translate(" (v/->str shift) ")")
                reverse-transform (str "translate(" (-> shift (v/mul -1) v/->str) ")"
                                       "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                       "rotate(" (- angle) ")"
                                       "translate(" (-> origin-point (v/mul -1) v/->str) ")")]
            [:g {:transform transform}
             (when (-> fimbriation :mode #{:double})
               (let [thickness (+ (-> fimbriation
                                      :thickness-1
                                      ((util/percent-of positional-charge-width)))
                                  (-> fimbriation
                                      :thickness-2
                                      ((util/percent-of positional-charge-width))))]
                 [:<>
                  (when outline?
                    [fimbriation/dilate-and-fill
                     adjusted-charge-without-shading
                     (+ thickness outline/stroke-width)
                     (outline/color context) context
                     :transform reverse-transform
                     :corner (-> fimbriation :corner)])
                  [fimbriation/dilate-and-fill
                   adjusted-charge-without-shading
                   (cond-> thickness
                     outline? (- outline/stroke-width))
                   (-> fimbriation
                       :tincture-2
                       (tincture/pick context)) context
                   :transform reverse-transform
                   :corner (-> fimbriation :corner)]]))
             (when (-> fimbriation :mode #{:single :double})
               (let [thickness (-> fimbriation
                                   :thickness-1
                                   ((util/percent-of positional-charge-width)))]
                 [:<>
                  (when outline?
                    [fimbriation/dilate-and-fill
                     adjusted-charge-without-shading
                     (+ thickness outline/stroke-width)
                     (outline/color context) context
                     :transform reverse-transform
                     :corner (-> fimbriation :corner)])
                  [fimbriation/dilate-and-fill
                   adjusted-charge-without-shading
                   (cond-> thickness
                     outline? (- outline/stroke-width))
                   (-> fimbriation
                       :tincture-1
                       (tincture/pick context)) context
                   :transform reverse-transform
                   :corner (-> fimbriation :corner)]]))

             (if preview-original?
               unadjusted-charge
               [:g
                [metadata/attribution
                 [:context :charge-data]
                 :charge {:charge-data full-charge-data}]
                (when render-field?
                  [:g {:mask (str "url(#" mask-inverted-id ")")}
                   [:g {:transform reverse-transform}
                    [field-shared/render (conj path :field) charge-environment context]]])
                [:g {:mask (str "url(#" mask-id ")")
                     ;; TODO: select component
                     :on-click nil #_(when fn-select-component
                                       (fn [event]
                                         (fn-select-component (-> context
                                                                  :db-path
                                                                  (conj :field)))
                                         (.stopPropagation event)))}
                 (svg/make-unique-ids coloured-charge)]
                (when render-shadow?
                  [:g {:mask (str "url(#" shadow-mask-id ")")}
                   [:rect {:transform reverse-transform
                           :x -500
                           :y -500
                           :width 1100
                           :height 1100
                           :fill "#001040"
                           :style {:opacity (:shadow tincture)}}]])

                (when render-highlight?
                  [:g {:mask (str "url(#" highlight-mask-id ")")}
                   [:rect {:transform reverse-transform
                           :x -500
                           :y -500
                           :width 1100
                           :height 1100
                           :fill "#ffffe8"
                           :style {:opacity (:highlight tincture)}}]])])])]])
      [:<>])))
