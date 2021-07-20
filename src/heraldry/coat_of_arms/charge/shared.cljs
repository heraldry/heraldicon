(ns heraldry.coat-of-arms.charge.shared
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.options :as options]
            [heraldry.util :as util]))

(defn make-charge
  [path _parent-path environment
   {:keys [charge-group] :as context} arg function]
  (let [;; TODO: bring this back
        ;; this is a bit hacky, but allows
        ;; overriding the origin point
        #_(update-in
           (charge-options/options charge)
           [:origin :point :choices]
           conj ["Special" :special])
        origin (options/sanitized-value (conj path :origin) context)
        anchor (options/sanitized-value (conj path :anchor) context)
        fimbriation (options/sanitized-value (conj path :fimbriation) context)
        size (options/sanitized-value (conj path :geometry :size) context)
        stretch (options/sanitized-value (conj path :geometry :stretch) context)
        mirrored? (options/sanitized-value (conj path :geometry :mirrored?) context)
        reversed? (options/sanitized-value (conj path :geometry :reversed?) context)
        squiggly? (options/render-option :squiggly? context)
        {:keys [charge-group
                slot-spacing
                slot-angle]} charge-group
        context (dissoc context :charge-group)
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
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
        ;; TODO: fix hint/outline-mode
        ;; hints (if (-> render-options :mode (= :hatching))
        ;;         (assoc hints :outline-mode :keep)
        ;;         hints)
        ;; outline? (or (:outline? render-options)
        ;;              (-> hints :outline-mode (= :keep)))
        outline? true
        angle (+ (v/angle-to-point origin-point anchor-point)
                 90)
        arg-value (get environment arg)

        ;; since size now is filled with a default, check whether it was set at all,
        ;; if not, then use nil
        ;; TODO: this probably needs a better mechanism and form representation
        size (if (options/raw-value (conj path :geometry :size) context) size nil)
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
                                (v// -2)))
        charge-shape (-> shape
                         svg/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                          squiggly? svg/squiggly-path
                          (not= angle 0) (->
                                          (svgpath)
                                          (.rotate angle)
                                          (.toString)))
                         (svg/translate (:x origin-point) (:y origin-point)))
        mask-shape (when mask
                     (-> mask
                         svg/make-path
                         (->
                          (svgpath)
                          (.scale scale-x scale-y)
                          (.toString))
                         (cond->
                          squiggly? svg/squiggly-path
                          (not= angle 0) (->
                                          (svgpath)
                                          (.rotate angle)
                                          (.toString)))
                         (svg/translate (:x origin-point) (:y origin-point))))
        [min-x max-x min-y max-y] (svg/rotated-bounding-box charge-top-left
                                                            (v/+ charge-top-left
                                                                 (v/v charge-width
                                                                      charge-height))
                                                            angle
                                                            :middle (v/v 0 0)
                                                            :scale (v/v scale-x scale-y))
        part [charge-shape
              [(v/+ origin-point
                    (v/v min-x min-y))
               (v/+ origin-point
                    (v/v max-x max-y))]
              mask-shape]
        ;; TODO: counterchanged
        ;; field (if (:counterchanged? field)
        ;;         (counterchange/counterchange-field charge parent-path :charge-group charge-group)
        ;;         field)
        charge-id (util/id "charge")
        environment (update environment :points dissoc :special)]
    [:<>
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
             outline/color context
             :corner (-> fimbriation :corner)])
          [fimbriation/dilate-and-fill-path
           charge-shape
           mask-shape
           (cond-> thickness
             outline? (- outline/stroke-width))
           (-> fimbriation
               :tincture-2
               (tincture/pick2 context)) context
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
             outline/color context
             :corner (-> fimbriation :corner)])
          [fimbriation/dilate-and-fill-path
           charge-shape
           mask-shape
           (cond-> thickness
             outline? (- outline/stroke-width))
           (-> fimbriation
               :tincture-1
               (tincture/pick2 context)) context
           :corner (-> fimbriation :corner)]]))
     [:g {:id charge-id}
      [field-shared/make-subfield
       (conj path :field) part
       :all
       environment context]
      (when outline?
        [:g outline/style
         [:path {:d charge-shape}]
         (when mask-shape
           [:path {:d mask-shape}])])]]))
