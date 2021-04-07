(ns heraldry.coat-of-arms.charge.other
  (:require [clojure.string :as s]
            [clojure.walk :as walk]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.environment :as environment]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
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
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (or (-> % second (s/starts-with? "url"))
                               (->> % second svg/normalize-colour (get placeholder-colours) (= :outline))))
                    [(first %) "none"]
                    %)
                 data))

(defn remove-shading [data placeholder-colours]
  (walk/postwalk #(if (and (vector? %)
                           (->> % first (get #{:stroke :fill :stop-color}))
                           (or (-> % second (s/starts-with? "url"))
                               (->> % second svg/normalize-colour (get placeholder-colours) #{:shadow :highlight})))
                    [(first %) "none"]
                    %)
                 data))

(defn replace-colours [data function]
  (walk/postwalk #(if (and (vector? %)
                           (-> % second string?)
                           (->> % first (get #{:stroke :fill :stop-color}))
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
  (let [mask-id (util/id "mask")
        mask-inverted-id (util/id "mask")
        mask (replace-colours
              data
              (fn [colour]
                (if (s/starts-with? colour "url")
                  "none"
                  (let [colour-lower (svg/normalize-colour colour)
                        kind (get placeholder-colours colour-lower)
                        replacement (get-replacement kind provided-placeholder-colours)]
                    (if (or (= kind :keep)
                            (and (not (#{:transparent :primary} outline-mode))
                                 (= kind :outline))
                            replacement)
                      "#fff"
                      "#000")))))
        mask-inverted (replace-colours
                       data
                       (fn [colour]
                         (if (s/starts-with? colour "url")
                           "none"
                           (let [colour-lower (svg/normalize-colour colour)
                                 kind (get placeholder-colours colour-lower)
                                 replacement (get-replacement kind provided-placeholder-colours)]
                             (if (or (= kind :keep)
                                     (and (not (#{:primary} outline-mode))
                                          (= kind :outline))
                                     replacement)
                               "#000"
                               "#fff")))))]
    [mask-id mask mask-inverted-id mask-inverted]))

(defn render [{:keys [field
                      tincture
                      hints
                      variant
                      data]
               :as charge}
              parent
              environment
              {:keys [render-field
                      render-options
                      load-charge-data
                      fn-select-component]
               :as context}]
  (let [full-charge-data (or data (when variant (load-charge-data variant)))]
    (if (:data full-charge-data)
      (let [{:keys [position
                    geometry
                    fimbriation]} (options/sanitize charge (charge-options/options charge))
            {:keys [size stretch
                    mirrored? reversed?
                    rotation]} geometry
            charge-data (:data full-charge-data)
            render-field? (-> charge-data
                              :fixed-tincture
                              (or :none)
                              (= :none))
            ;; since size now is filled with a default, check whether it was set at all,
            ;; if not, then use nil
            ;; TODO: this probably needs a better mechanism and form representation
            size (if (-> charge :geometry :size) size nil)
            points (:points environment)
            top (:top points)
            bottom (:bottom points)
            left (:left points)
            right (:right points)
            positional-charge-width (js/parseFloat (-> charge-data :width (or "1")))
            positional-charge-height (js/parseFloat (-> charge-data :height (or "1")))
            width (:width environment)
            height (:height environment)
            center-point (position/calculate position environment :fess)
            min-x-distance (min (- (:x center-point) (:x left))
                                (- (:x right) (:x center-point)))
            min-y-distance (min (- (:y center-point) (:y top))
                                (- (:y bottom) (:y center-point)))
            target-width (if size
                           (-> size
                               ((util/percent-of width)))
                           (* (* min-x-distance 2) 0.8))
            target-height (/ (if size
                               (-> size
                                   ((util/percent-of height)))
                               (* (* min-y-distance 2) 0.7))
                             stretch)
            scale-x (* (if mirrored? -1 1)
                       (min (/ target-width positional-charge-width)
                            (/ target-height positional-charge-height)))
            scale-y (* (if reversed? -1 1)
                       (* (Math/abs scale-x) stretch))
            placeholder-colours (:colours
                                 full-charge-data)
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
            unadjusted-charge (-> charge-data
                                  :data
                                  svg/fix-string-style-values)
            adjusted-charge (-> unadjusted-charge
                                (cond->
                                 (not (or (-> hints :outline-mode (not= :remove))
                                          (:outline? render-options))) (remove-outlines placeholder-colours)))
            adjusted-charge-without-shading (-> adjusted-charge
                                                (remove-shading placeholder-colours))
            [mask-id mask
             mask-inverted-id mask-inverted] (make-mask adjusted-charge-without-shading
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
            coloured-charge (replace-colours
                             adjusted-charge-without-shading
                             (fn [colour]
                               (let [colour-lower (svg/normalize-colour colour)
                                     kind (get placeholder-colours colour-lower)
                                     replacement (get-replacement kind tincture)]
                                 (cond
                                   replacement (tincture/pick replacement render-options)
                                   (= kind :keep) colour
                                   :else "#000000"))))
            shift (-> (v/v positional-charge-width positional-charge-height)
                      (v// 2)
                      (v/-))
            [min-x max-x min-y max-y] (svg/rotated-bounding-box
                                       shift
                                       (v/dot shift (v/v -1 -1))
                                       rotation
                                       :scale (v/v scale-x scale-y))
            clip-size (v/v (- max-x min-x) (- max-y min-y))
            position (-> clip-size
                         (v/-)
                         (v// 2)
                         (v/+ center-point))
            charge-environment (environment/create
                                (svg/make-path ["M" position
                                                "l" (v/v (:x clip-size) 0)
                                                "l" (v/v 0 (:y clip-size))
                                                "l" (v/v (- (:x clip-size)) 0)
                                                "l" (v/v 0 (- (:y clip-size)))
                                                "z"])
                                {:parent field
                                 :parent-environment environment
                                 :context [:charge]
                                 :bounding-box (svg/bounding-box
                                                [position (v/+ position
                                                               clip-size)])
                                 :override-environment (when (or (:inherit-environment? field)
                                                                 (:counterchanged? field))
                                                         environment)})
            field (if (:counterchanged? field)
                    (counterchange/counterchange-field charge parent)
                    field)
            charge-name (or (:name full-charge-data) "")
            username (:username full-charge-data)
            charge-url (or (util/full-url-for-charge full-charge-data) "")
            attribution (:attribution full-charge-data)
            outline? (or (:outline? render-options)
                         (-> hints :outline-mode (= :keep)))]
        [:<>
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
                     (let [colour-lower (svg/normalize-colour colour)
                           kind (get placeholder-colours colour-lower)]
                       (case kind
                         :outline "#000000"
                         :shadow "none"
                         :highlight "none"
                         "#ffffff")))))
                svg/make-unique-ids)]]
             [:g {:mask (str "url(#" shadow-helper-mask-id ")")}
              (->
               adjusted-charge
               (replace-colours
                (fn [colour]
                  (if (s/starts-with? colour "url")
                    colour
                    (let [colour-lower (svg/normalize-colour colour)
                          kind (get placeholder-colours colour-lower)]
                      (case kind
                        :shadow "#ffffff"
                        :highlight "none"
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
                     (let [colour-lower (svg/normalize-colour colour)
                           kind (get placeholder-colours colour-lower)]
                       (case kind
                         :outline "#000000"
                         :shadow "none"
                         :highlight "none"
                         "#ffffff")))))
                svg/make-unique-ids)]]
             [:g {:mask (str "url(#" highlight-helper-mask-id ")")}
              (->
               adjusted-charge
               (replace-colours
                (fn [colour]
                  (if (s/starts-with? colour "url")
                    colour
                    (let [colour-lower (svg/normalize-colour colour)
                          kind (get placeholder-colours colour-lower)]
                      (case kind
                        :shadow "none"
                        :highlight "#ffffff"
                        "#000000")))))
               svg/make-unique-ids)]])
          [:mask {:id mask-id}
           (svg/make-unique-ids mask)]
          [:mask {:id mask-inverted-id}
           (svg/make-unique-ids mask-inverted)]]
         (let [transform (str "translate(" (:x center-point) "," (:y center-point) ")"
                              "rotate(" rotation ")"
                              "scale(" scale-x "," scale-y ")"
                              "translate(" (-> shift :x) "," (-> shift :y) ")")
               reverse-transform (str "translate(" (-> shift :x -) "," (-> shift :y -) ")"
                                      "scale(" (/ 1 scale-x) "," (/ 1 scale-y) ")"
                                      "rotate(" (- rotation) ")"
                                      "translate(" (- (:x center-point)) "," (- (:y center-point)) ")")]
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
                    outline/color render-options
                    :transform reverse-transform
                    :corner (-> fimbriation :corner)])
                 [fimbriation/dilate-and-fill
                  adjusted-charge-without-shading
                  (cond-> thickness
                    outline? (- outline/stroke-width))
                  (-> fimbriation
                      :tincture-2
                      (tincture/pick render-options)) render-options
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
                    outline/color render-options
                    :transform reverse-transform
                    :corner (-> fimbriation :corner)])
                 [fimbriation/dilate-and-fill
                  adjusted-charge-without-shading
                  (cond-> thickness
                    outline? (- outline/stroke-width))
                  (-> fimbriation
                      :tincture-1
                      (tincture/pick render-options)) render-options
                  :transform reverse-transform
                  :corner (-> fimbriation :corner)]]))

            (if (:preview-original? render-options)
              unadjusted-charge
              [:g
               [metadata/attribution charge-name username (util/full-url-for-username username) charge-url attribution]
               (when render-field?
                 [:g {:mask (str "url(#" mask-inverted-id ")")}
                  [:g {:transform reverse-transform}
                   [render-field field charge-environment (-> context
                                                              (update :db-path conj :field)
                                                              (dissoc :fn-select-component))]]])
               [:g {:mask (str "url(#" mask-id ")")
                    :on-click (when fn-select-component
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
                          :fill "#000000"
                          :style {:opacity (:shadow tincture)}}]])

               (when render-highlight?
                 [:g {:mask (str "url(#" highlight-mask-id ")")}
                  [:rect {:transform reverse-transform
                          :x -500
                          :y -500
                          :width 1100
                          :height 1100
                          :fill "#ffffff"
                          :style {:opacity (:highlight tincture)}}]])])])])
      [:<>])))
