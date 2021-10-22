(ns heraldry.coat-of-arms.charge.shared
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.interface :as interface]
            [heraldry.math.bounding-box :as bounding-box]
            [heraldry.math.svg.path :as path]
            [heraldry.math.svg.squiggly :as squiggly]
            [heraldry.math.vector :as v]
            [heraldry.util :as util]))

(defn make-charge
  [path _parent-path environment
   {:keys [charge-group
           origin-override
           size-default] :as context} arg function]
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
        squiggly? (interface/render-option :squiggly? context)
        outline-mode (if (or (interface/render-option :outline? context)
                             (= (interface/render-option :mode context)
                                :hatching)) :keep
                         (interface/get-sanitized-data (conj path :outline-mode) context))
        outline? (= outline-mode :keep)
        {:keys [charge-group-path
                slot-spacing
                slot-angle]} charge-group
        context (dissoc context :charge-group)
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
        points (:points environment)
        top (:top points)
        bottom (:bottom points)
        left (:left points)
        right (:right points)
        width (:width environment)
        height (:height environment)
        angle (+ (v/angle-to-point origin-point anchor-point)
                 90)
        arg-value (get environment arg)

        ;; since size now is filled with a default, check whether it was set at all,
        ;; if not, then use nil
        ;; TODO: this probably needs a better mechanism and form representation
        size (if (interface/get-raw-data (conj path :geometry :size) context) size nil)
        target-arg-value (-> (or size
                                 80)
                             ((util/percent-of arg-value)))
        {:keys [shape
                mask
                charge-width
                charge-height
                charge-top-left]} (function target-arg-value)
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
                (if (< charge-height charge-width)
                  (+ angle slot-angle 90)
                  (+ angle slot-angle))
                angle)
        scale-x (* (if mirrored? -1 1)
                   (min (/ target-width charge-width)
                        (/ target-height charge-height)))
        scale-y (* (if reversed? -1 1)
                   (* (Math/abs scale-x) stretch))
        charge-top-left (or charge-top-left
                            (-> (v/v charge-width charge-height)
                                (v/div -2)))
        charge-shape (-> shape
                         path/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                           squiggly? squiggly/squiggly-path
                           (not= angle 0) (->
                                           (svgpath)
                                           (.rotate angle)
                                           (.toString)))
                         (path/translate (:x origin-point) (:y origin-point)))
        mask-shape (when mask
                     (-> mask
                         path/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                          squiggly? squiggly/squiggly-path
                          (not= angle 0) (->
                                          (svgpath)
                                          (.rotate angle)
                                          (.toString)))
                         (path/translate (:x origin-point) (:y origin-point))))
        [min-x max-x min-y max-y] (bounding-box/rotate charge-top-left
                                                       (v/add charge-top-left
                                                              (v/v charge-width
                                                                   charge-height))
                                                       angle
                                                       :middle (v/v 0 0)
                                                       :scale (v/v scale-x scale-y))
        part [charge-shape
              [(v/add origin-point
                      (v/v min-x min-y))
               (v/add origin-point
                      (v/v max-x max-y))]
              mask-shape]
        charge-id (util/id "charge")
        vertical-mask? (not (zero? vertical-mask))
        vertical-mask-id (util/id "mask")]
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
      (when (-> fimbriation :mode #{:double})
        (let [thickness (+ (-> fimbriation
                               :thickness-1
                               ((util/percent-of charge-width)))
                           (-> fimbriation
                               :thickness-2
                               ((util/percent-of charge-width))))]
          [:<>
           (when outline?
             [fimbriation/dilate-and-fill-path
              charge-shape
              mask-shape
              (+ thickness outline/stroke-width)
              (outline/color context) context
              :corner (-> fimbriation :corner)])
           [fimbriation/dilate-and-fill-path
            charge-shape
            mask-shape
            (cond-> thickness
              outline? (- outline/stroke-width))
            (-> fimbriation
                :tincture-2
                (tincture/pick context)) context
            :corner (-> fimbriation :corner)]]))
      (when (-> fimbriation :mode #{:single :double})
        (let [thickness (-> fimbriation
                            :thickness-1
                            ((util/percent-of charge-width)))]
          [:<>
           (when outline?
             [fimbriation/dilate-and-fill-path
              charge-shape
              mask-shape
              (+ thickness outline/stroke-width)
              (outline/color context) context
              :corner (-> fimbriation :corner)])
           [fimbriation/dilate-and-fill-path
            charge-shape
            mask-shape
            (cond-> thickness
              outline? (- outline/stroke-width))
            (-> fimbriation
                :tincture-1
                (tincture/pick context)) context
            :corner (-> fimbriation :corner)]]))
      [:g {:id charge-id}
       [field-shared/make-subfield
        (conj path :field) part
        :all
        environment context]
       (when outline?
         [:g (outline/style context)
          [:path {:d charge-shape}]
          (when mask-shape
            [:path {:d mask-shape}])])]]]))
