(ns heraldicon.render.ribbon
  (:require
   [clojure.string :as s]
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.frontend.js-event :as js-event]
   [heraldicon.heraldry.ribbon :as ribbon]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.localization.string :as string]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.svg.path :as path]
   [heraldicon.util.colour :as colour]
   [heraldicon.util.uid :as uid]))

(defn render [{:keys [select-component-fn
                      svg-export?] :as context}
              tincture-foreground
              tincture-background
              tincture-text
              & {:keys [outline-thickness]
                 :or {outline-thickness 1}}]
  (let [thickness (interface/get-sanitized-data (c/++ context :thickness))
        edge-angle (interface/get-sanitized-data (c/++ context :edge-angle))
        end-split (interface/get-sanitized-data (c/++ context :end-split))
        outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        points (interface/get-raw-data (c/++ context :points))
        segments (interface/get-raw-data (c/++ context :segments))
        {:keys [curves edge-vectors]} (ribbon/generate-curves points edge-angle)
        num-curves (count curves)
        foreground-colour (tincture/pick tincture-foreground context)
        background-colour (if (= tincture-background :none)
                            (colour/darken foreground-colour)
                            (tincture/pick tincture-background context))
        text-colour (tincture/pick tincture-text context)]
    (into [:g (when-not svg-export?
                {:on-click (when select-component-fn
                             (js-event/handled
                              #(select-component-fn (c/-- context))))
                 :style {:cursor "pointer"}})]
          (map (fn [[idx partial-curve]]
                 (let [top-edge (path/curve-to-relative partial-curve)
                       [first-edge-vector second-edge-vector] (get edge-vectors idx)
                       first-edge-vector (v/mul first-edge-vector thickness)
                       second-edge-vector (v/mul second-edge-vector thickness)
                       full-path (cond
                                   (or (zero? end-split)
                                       (< 0 idx (dec num-curves))) (str top-edge
                                                                        (path/line-to second-edge-vector)
                                                                        (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                                        (path/line-to (v/mul first-edge-vector -1)))
                               ;; special case of only one segment with end-split > 0
                                   (and (pos? end-split)
                                        (zero? idx)
                                        (= idx (dec num-curves))) (str top-edge
                                                                       (string/combine " "
                                                                                       (ribbon/split-end
                                                                                        :end
                                                                                        partial-curve
                                                                                        (/ end-split 2)
                                                                                        second-edge-vector))
                                                                       (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                                       (string/combine " "
                                                                                       (ribbon/split-end
                                                                                        :start
                                                                                        partial-curve
                                                                                        (/ end-split 2)
                                                                                        first-edge-vector)))
                                   (and (pos? end-split)
                                        (zero? idx)) (str top-edge
                                                          (path/line-to second-edge-vector)
                                                          (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                          (string/combine " "
                                                                          (ribbon/split-end
                                                                           :start
                                                                           partial-curve
                                                                           end-split
                                                                           first-edge-vector)))
                                   (and (pos? end-split)
                                        (= idx (dec num-curves))) (str top-edge
                                                                       (string/combine " "
                                                                                       (ribbon/split-end
                                                                                        :end
                                                                                        partial-curve
                                                                                        end-split
                                                                                        second-edge-vector))
                                                                       (ribbon/project-bottom-edge partial-curve first-edge-vector second-edge-vector)
                                                                       (path/line-to (v/mul first-edge-vector -1))))
                       segment-context (c/++ context :segments idx)
                       segment-type (interface/get-raw-data (c/++ segment-context :type))
                       foreground? (#{:heraldry.ribbon.segment.type/foreground
                                      :heraldry.ribbon.segment.type/foreground-with-text} segment-type)
                       text (some-> (interface/get-sanitized-data (c/++ segment-context :text))
                                    (s/replace #"[*]" "⬪"))
                       text? (and (= segment-type :heraldry.ribbon.segment.type/foreground-with-text)
                                  (some-> text
                                          s/trim
                                          count
                                          pos?))]
                   ^{:key idx}
                   [:g
                    [:path {:d full-path
                            :style (merge (when outline?
                                            (outline/style context))
                                          (when outline?
                                            {:stroke-width outline-thickness})
                                          {:fill (if foreground?
                                                   foreground-colour
                                                   background-colour)})}]
                    (when text?
                      (let [path-id (uid/generate "path")
                            spacing (interface/get-sanitized-data (c/++ segment-context :spacing))
                            offset-x (interface/get-sanitized-data (c/++ segment-context :offset-x))
                            offset-y (interface/get-sanitized-data (c/++ segment-context :offset-y))
                            font (some-> (interface/get-sanitized-data (c/++ segment-context :font))
                                         font/css-string)
                            font-scale (interface/get-sanitized-data (c/++ segment-context :font-scale))
                            font-size (* font-scale thickness)
                            spacing (* spacing font-size)
                            text-path-start (v/add (ffirst partial-curve)
                                                   (v/mul first-edge-vector (- 0.6 offset-y)))
                            text-path-end (v/add (last (last partial-curve))
                                                 (v/mul second-edge-vector (- 0.6 offset-y)))
                            text-path (ribbon/project-path-to
                                       partial-curve
                                       text-path-start
                                       text-path-end)]
                        [:text.no-select {:fill text-colour
                                          :text-anchor "middle"
                                          :style {:font-family font
                                                  :font-size font-size}}
                         [:defs
                          [:path {:id path-id
                                  :d (str "M " (v/->str text-path-start) " " text-path)}]]
                         [:textPath {:href (str "#" path-id)
                                     :alignment-baseline "middle"
                                     :method "align"
                                     :lengthAdjust "spacing"
                                     :letter-spacing spacing
                                     :startOffset (str (+ 50 (* offset-x 100)) "%")}
                          text]]))])))
          (sort-by (fn [[idx _]]
                     [(or (:z-index (get segments idx))
                          1000)
                      idx])
                   (map-indexed vector curves)))))

(defn render-standalone [{:keys [svg-export?
                                 target-width
                                 target-height
                                 embed-fonts] :as context}]
  (let [points (interface/get-raw-data (c/++ context :points))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
        ;; assumed to be (0 thickness) as a max) needs to be added to every point for the correct
        ;; height; could perhaps be a subscription or the ribbon function can provide it?
        ;; but then can't use the ribbon function as reagent component
        {:keys [min-x max-x
                min-y max-y]} (bb/from-points
                               (concat points
                                       (map (partial v/add (v/Vector. 0 thickness)) points)))
        margin 10
        [result-width result-height] [(+ (- max-x min-x)
                                         (* 2 margin))
                                      (+ (- max-y min-y)
                                         (* 2 margin))]
        [document-width
         document-height
         document-scale] (if (and svg-export?
                                  (or target-width
                                      target-height))
                           (let [scale-width (when target-width
                                               (/ target-width result-width))
                                 scale-height (when target-height
                                                (/ target-height result-height))
                                 scale (or (when (and scale-width scale-height)
                                             (min scale-width scale-height))
                                           scale-width
                                           scale-height)]
                             [(* result-width scale)
                              (* result-height scale)
                              scale])
                           [result-width
                            result-height
                            1])
        used-fonts (into #{}
                         (keep (fn [j]
                                 (interface/get-sanitized-data (c/++ context :segments j :font))))
                         (range (interface/get-list-size (c/++ context :segments))))]
    [:svg (merge
           {:viewBox (str "0 0 " document-width " " document-height)}
           (if svg-export?
             {:xmlns "http://www.w3.org/2000/svg"
              :version "1.1"
              :width document-width
              :height document-height}
             {:style {:width "100%"}
              :preserveAspectRatio "xMidYMin meet"}))
     (when (and svg-export?
                embed-fonts)
       [embed-fonts used-fonts])
     [:g {:transform (str "scale(" document-scale "," document-scale ")")}
      [:g {:transform (str "translate(" (- (- min-x margin)) "," (- (- min-y margin)) ")")}
       [render context :argent :none :helmet-dark]]]]))
