(ns heraldry.coat-of-arms.charge.shared
  (:require ["svgpath" :as svgpath]
            [heraldry.coat-of-arms.charge.options :as charge-options]
            [heraldry.coat-of-arms.counterchange :as counterchange]
            [heraldry.coat-of-arms.division.shared :as division-shared]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

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
