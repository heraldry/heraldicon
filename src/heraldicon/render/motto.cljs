(ns heraldicon.render.motto
  (:require
   [heraldicon.context :as c]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.render.ribbon :as ribbon]))

(defn render [{:keys [environment
                      self-below-shield?
                      render-pass-below-shield?]
               :as context}]
  (let [{:keys [width height]} environment
        points (interface/get-raw-data (c/++ context :ribbon :points))]
    (if (and (= (boolean self-below-shield?)
                (boolean render-pass-below-shield?))
             points)
      (let [anchor-point (interface/get-sanitized-data (c/++ context :anchor :point))
            tincture-foreground (interface/get-sanitized-data (c/++ context :tincture-foreground))
            tincture-background (interface/get-sanitized-data (c/++ context :tincture-background))
            tincture-text (interface/get-sanitized-data (c/++ context :tincture-text))
            offset-x (interface/get-sanitized-data (c/++ context :anchor :offset-x))
            offset-y (interface/get-sanitized-data (c/++ context :anchor :offset-y))
            size (interface/get-sanitized-data (c/++ context :geometry :size))
            thickness (interface/get-sanitized-data (c/++ context :ribbon :thickness))
            position (v/add (-> environment :points (get anchor-point))
                            (v/Vector. (math/percent-of width offset-x)
                                       (- (math/percent-of height offset-y))))
            ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
            ;; assumed to be (0 thickness) as a max) needs to be added to every point for the correct
            ;; height; could perhaps be a subscription or the ribbon function can provide it?
            ;; but then can't use the ribbon function as reagent component
            {:keys [min-x max-x
                    min-y max-y]} (bb/from-points
                                   (concat points
                                           (map (partial v/add (v/Vector. 0 thickness)) points)))
            ribbon-width (- max-x min-x)
            ribbon-height (- max-y min-y)
            target-width (math/percent-of width size)
            scale (/ target-width ribbon-width)
            outline-thickness (/ outline/stroke-width
                                 2
                                 scale)]
        {:result [:g {:transform (str "translate(" (v/->str position) ")"
                                      "scale(" scale "," scale ")"
                                      "translate(" (v/->str (-> (v/Vector. ribbon-width ribbon-height)
                                                                (v/div 2)
                                                                (v/add (v/Vector. min-x min-y))
                                                                (v/mul -1))) ")")}
                  [ribbon/render
                   (c/++ context :ribbon)
                   tincture-foreground
                   tincture-background
                   tincture-text
                   :outline-thickness outline-thickness]]
         :bounding-box (bb/from-points
                        [(-> (v/Vector. min-x min-y)
                             (v/mul scale)
                             (v/add position))
                         (-> (v/Vector. max-x max-y)
                             (v/mul scale)
                             (v/add position))])})
      {:result nil
       :bounding-box nil})))
