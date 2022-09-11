(ns heraldicon.render.charge
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.escutcheon :as escutcheon]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]))

(defn render-standalone [context]
  (let [{:keys [svg-export?
                target-width
                target-height]} (c/render-hints context)
        escutcheon-data (escutcheon/data :rectangle nil nil nil nil nil)
        {:keys [environment]} (escutcheon/transform-to-width escutcheon-data 100)
        charge-context (c/set-key context :parent-environment-override environment)
        bounding-box (interface/get-bounding-box charge-context)
        [width height] (bb/size bounding-box)
        target-width (or target-width 1000)
        scale (if (and svg-export?
                       (or target-width
                           target-height))
                (let [scale-width (when target-width
                                    (/ target-width width))
                      scale-height (when target-height
                                     (/ target-height height))]
                  (or (when (and scale-width scale-height)
                        (min scale-width scale-height))
                      scale-width
                      scale-height))
                1)
        [document-width document-height] [(* width scale) (* height scale)]]
    [:svg (merge
           {:viewBox (bb/->viewbox bounding-box :margin 2)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :xmlnsXlink "http://www.w3.org/1999/xlink"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     [interface/render-component charge-context]]))
