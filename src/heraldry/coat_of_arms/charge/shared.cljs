(ns heraldry.coat-of-arms.charge.shared
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.angle :as angle]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.field.shared :as field-shared]
            [heraldry.coat-of-arms.line.fimbriation :as fimbriation]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn make-charge
  [{:keys [field hints] :as charge} parent environment {:keys [render-options] :as context} arg function]
  (let [{:keys [origin
                anchor
                geometry
                fimbriation]} (options/sanitize charge (charge-options/options charge))
        {:keys [size stretch
                mirrored? reversed?]} geometry
        {origin-point :real-origin
         anchor-point :real-anchor} (angle/calculate-origin-and-anchor
                                     environment
                                     origin
                                     anchor
                                     0
                                     -90)
        hints (if (-> render-options :mode (= :hatching))
                (assoc hints :outline-mode :keep)
                hints)
        angle (+ (v/angle-to-point origin-point anchor-point)
                 90)
        arg-value (get environment arg)
        target-arg-value (-> size
                             ((util/percent-of arg-value)))
        scale-x (if mirrored? -1 1)
        scale-y (* (if reversed? -1 1) stretch)
        {:keys [shape
                mask
                charge-width
                charge-height
                charge-top-left]} (function target-arg-value)
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
                          (:squiggly? render-options) svg/squiggly-path
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
                          (:squiggly? render-options) svg/squiggly-path
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
        parts [[charge-shape
                [(v/+ origin-point
                      (v/v min-x min-y))
                 (v/+ origin-point
                      (v/v max-x max-y))]
                mask-shape]]
        field (if (:counterchanged? field)
                (counterchange/counterchange-field charge parent)
                field)
        charge-id (util/id "charge")
        outline? (or (:outline? render-options)
                     (-> hints :outline-mode (= :keep)))]
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
             outline/color render-options
             :corner (-> fimbriation :corner)])
          [fimbriation/dilate-and-fill-path
           charge-shape
           mask-shape
           (cond-> thickness
             outline? (- outline/stroke-width))
           (-> fimbriation
               :tincture-2
               (tincture/pick render-options)) render-options
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
             outline/color render-options
             :corner (-> fimbriation :corner)])
          [fimbriation/dilate-and-fill-path
           charge-shape
           mask-shape
           (cond-> thickness
             outline? (- outline/stroke-width))
           (-> fimbriation
               :tincture-1
               (tincture/pick render-options)) render-options
           :corner (-> fimbriation :corner)]]))
     [:g {:id charge-id}
      [field-shared/make-subfields
       :charge-pale [field] parts
       [:all]
       environment charge context]
      (when outline?
        [:g outline/style
         [:path {:d charge-shape}]
         (when mask-shape
           [:path {:d mask-shape}])])]]))
