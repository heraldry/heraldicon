(ns heraldry.coat-of-arms.division.type.per-pale
  (:require [heraldry.coat-of-arms.division.options :as division-options]
            [heraldry.coat-of-arms.division.shared :as shared]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.line.core :as line]
            [heraldry.coat-of-arms.options :as options]
            [heraldry.coat-of-arms.position :as position]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.vector :as v]))

(defn render
  {:display-name "Per pale"
   :value        :per-pale
   :parts        ["dexter" "sinister"]}
  [{:keys [type fields hints] :as division} environment {:keys [render-options] :as context}]
  (let [{:keys [line origin]}          (options/sanitize division (division-options/options division))
        points                         (:points environment)
        origin-point                   (position/calculate origin environment :fess)
        top-left                       (:top-left points)
        top                            (assoc (:top points) :x (:x origin-point))
        bottom                         (assoc (:bottom points) :x (:x origin-point))
        bottom-right                   (:bottom-right points)
        {line-one       :line
         line-one-start :line-start
         line-one-end   :line-end
         :as            line-one-data} (line/create line
                                                    (:y (v/- bottom top))
                                                    :angle 90
                                                    :render-options render-options)
        parts                          [[["M" (v/+ top
                                                   line-one-start)
                                          (svg/stitch line-one)
                                          (infinity/path :clockwise
                                                         [:bottom :top]
                                                         [(v/+ bottom
                                                               line-one-end)
                                                          (v/+ top
                                                               line-one-start)])
                                          "z"]
                                         [top-left
                                          bottom]]

                                        [["M" (v/+ top
                                                   line-one-start)
                                          (svg/stitch line-one)
                                          (infinity/path :counter-clockwise
                                                         [:bottom :top]
                                                         [(v/+ bottom
                                                               line-one-end)
                                                          (v/+ top
                                                               line-one-start)])
                                          "z"]
                                         [top
                                          bottom-right]]]
        outline? (or (:outline? render-options)
                     (:outline? hints))]
    [:<>
     [shared/make-division
      (shared/division-context-key type) fields parts
      [:all nil]
      environment division context]
     (line/render line [line-one-data] top outline? render-options)]))
