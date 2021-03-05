(ns heraldry.coat-of-arms.line.fimbriation
  (:require [clojure.walk :as walk]
            [heraldry.coat-of-arms.infinity :as infinity]
            [heraldry.coat-of-arms.outline :as outline]
            [heraldry.coat-of-arms.svg :as svg]
            [heraldry.coat-of-arms.tincture.core :as tincture]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.util :as util]))

(defn render-tinctured-area [tincture path {:keys [svg-export?] :as render-options}]
  (let [mask-id (util/id "mask")]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d (svg/make-path path)
               :fill "#ffffff"}]]]
     [:rect {:x -500
             :y -500
             :width 1100
             :height 1100
             :mask (str "url(#" mask-id ")")
             :fill (tincture/pick tincture render-options)
             :style (when (not svg-export?)
                      {:pointer-events "none"})}]]))

(defn render_ [[start start-direction]
               [end end-direction]
               lines
               {:keys [:tincture-1 :tincture-2]}
               render-options]
  (let [{fimbriation-1 :fimbriation-1
         fimbriation-2 :fimbriation-2
         first-line-start :line-start
         first-fimbriation-1-start :fimbriation-1-start
         first-fimbriation-2-start :fimbriation-2-start} (first lines)
        {last-line-end :line-end
         last-fimbriation-1-end :fimbriation-1-end
         last-fimbriation-2-end :fimbriation-2-end} (last lines)

        elements [:<> (when fimbriation-2
                        [render-tinctured-area
                         tincture-2
                         ["M" (v/+ start first-line-start)
                          (for [{:keys [line]} lines]
                            (svg/stitch line))
                          (if (= end-direction :none)
                            ["L" (v/+ end
                                      last-fimbriation-2-end)]
                            (infinity/path :counter-clockwise
                                           [end-direction end-direction]
                                           [(v/+ end
                                                 last-line-end)
                                            (v/+ end
                                                 last-fimbriation-2-end)]))
                          (for [{:keys [fimbriation-2]} (reverse lines)]
                            (svg/stitch fimbriation-2))
                          (if (= start-direction :none)
                            ["L" (v/+ start
                                      first-line-start)]
                            (infinity/path :counter-clockwise
                                           [start-direction start-direction]
                                           [(v/+ start
                                                 first-fimbriation-2-start)
                                            (v/+ start
                                                 first-line-start)]))
                          "z"]
                         render-options])

                  (when fimbriation-1
                    [render-tinctured-area
                     tincture-1
                     ["M" (v/+ start first-line-start)
                      (for [{:keys [line]} lines]
                        (svg/stitch line))
                      (if (= end-direction :none)
                        ["L" (v/+ end
                                  last-fimbriation-1-end)]
                        (infinity/path :counter-clockwise
                                       [end-direction end-direction]
                                       [(v/+ end
                                             last-line-end)
                                        (v/+ end
                                             last-fimbriation-1-end)]))
                      (for [{:keys [fimbriation-1]} (reverse lines)]
                        (svg/stitch fimbriation-1))
                      (if (= start-direction :none)
                        ["L" (v/+ start
                                  first-line-start)]
                        (infinity/path :counter-clockwise
                                       [start-direction start-direction]
                                       [(v/+ start
                                             first-fimbriation-1-start)
                                        (v/+ start
                                             first-line-start)]))
                      "z"]
                     render-options])]

        outlines [:<>
                  (when fimbriation-1
                    [:path {:d (svg/make-path
                                ["M" (v/+ end last-fimbriation-1-end)
                                 (for [{:keys [fimbriation-1]} (reverse lines)]
                                   (svg/stitch fimbriation-1))])}])

                  (when fimbriation-2
                    [:path {:d (svg/make-path
                                ["M" (v/+ end last-fimbriation-2-end)
                                 (for [{:keys [fimbriation-2]} (reverse lines)]
                                   (svg/stitch fimbriation-2))])}])]]
    [elements outlines]))

(defn linejoin [corner]
  (case corner
    :round "round"
    :sharp "miter"
    :bevel "bevel"
    "round"))

(defn dilate-and-fill-path [shape negate-shape thickness color {:keys [svg-export?]}
                            & {:keys [fill? corner] :or {fill? true}}]
  (let [mask-id (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:path {:d shape
               :fill (when fill? "#ffffff")
               :style {:stroke-width thickness
                       :stroke "#ffffff"
                       :stroke-linejoin linejoin-value
                       :stroke-miterlimit 10}}]
       (when negate-shape
         [:path {:d negate-shape
                 :fill "#000000"
                 :style {:stroke-width thickness
                         :stroke "#ffffff"
                         :stroke-linejoin linejoin-value
                         :stroke-miterlimit 10}}])]]

     [:rect {:x -500
             :y -500
             :width 1100
             :height 1100
             :mask (str "url(#" mask-id ")")
             :fill color
             :style (when (not svg-export?)
                      {:pointer-events "none"})}]]))

(defn dilate-recursively [data stroke-width color linejoin]
  (walk/postwalk #(cond
                    (and (vector? %)
                         (-> % first (= :stroke-width))) [(first %) stroke-width]
                    (and (vector? %)
                         (-> % first (= :style))) [(first %) (-> %
                                                                 second
                                                                 (conj [:stroke-width stroke-width]))]
                    (and (vector? %)
                         (-> % first (= :stroke-linejoin))) [(first %) linejoin]
                    (and (vector? %)
                         (-> % first #{:stroke :fill})) [(first %) color]
                    :else %)
                 data))

(defn dilate-and-fill [shape thickness color {:keys [svg-export?]}
                       & {:keys [transform corner]}]
  (let [mask-id (util/id "mask")
        linejoin-value (linejoin corner)]
    [:<>
     [:defs
      [:mask {:id mask-id}
       [:g {:fill "#ffffff"
            :style {:stroke-width thickness
                    :stroke "#ffffff"
                    :stroke-linejoin linejoin-value
                    :stroke-miterlimit 10}}
        (dilate-recursively shape thickness "#ffffff" linejoin-value)]]]
     [:g {:mask (str "url(#" mask-id ")")}
      [:g {:transform transform}
       [:rect {:x -500
               :y -500
               :width 1100
               :height 1100
               :fill color
               :style (when (not svg-export?)
                        {:pointer-events "none"})}]]]]))

(defn render [line-path mask-id thickness color outline? corner render-options]
  [:g {:mask (when mask-id
               (str "url(#" mask-id ")"))}
   (when outline?
     [dilate-and-fill-path
      line-path
      nil
      (+ thickness
         outline/stroke-width)
      outline/color
      render-options
      :corner corner
      :fill? false])
   (let [effective-thickness (cond-> thickness
                               outline? (- outline/stroke-width))]
     (when (> effective-thickness 0)
       [dilate-and-fill-path
        line-path
        nil
        effective-thickness
        color
        render-options
        :corner corner
        :fill? false]))])

(defn draw-line [line line-data start outline? render-options]
  (let [{:keys [fimbriation]} line
        {line-path-snippet :line
         line-start :line-start
         line-up :up
         line-down :down} line-data
        line-path (svg/make-path ["M" (v/+ start line-start)
                                  (svg/stitch line-path-snippet)])
        {:keys [mode
                alignment
                thickness-1
                thickness-2
                tincture-1
                tincture-2
                corner]} fimbriation
        [thickness-1 thickness-2
         tincture-1 tincture-2] (if (and (= alignment :inside)
                                         (= mode :double))
                                  [thickness-2 thickness-1
                                   tincture-2 tincture-1]
                                  [thickness-1 thickness-2
                                   tincture-1 tincture-2])
        mask-shape-top (when (#{:even :outside} alignment)
                         (svg/make-path ["M" (v/+ start line-start)
                                         (svg/stitch line-path-snippet)
                                         "l" line-up
                                         "L" (v/+ start line-start line-up)
                                         "z"]))
        mask-shape-bottom (when (#{:even :inside} alignment)
                            (svg/make-path ["M" (v/+ start line-start)
                                            (svg/stitch line-path-snippet)
                                            "l" line-down
                                            "L" (v/+ start line-start line-down)
                                            "z"]))
        combined-thickness (+ thickness-1 thickness-2)
        mask-id-top (when mask-shape-top
                      (util/id "mask-line-top"))
        mask-id-bottom (when mask-shape-bottom
                         (util/id "mask-line-bottom"))]
    [:<>
     (when (#{:single :double} mode)
       [:<>
        (when (or mask-shape-top mask-shape-bottom)
          [:defs
           (when mask-shape-top
             [:mask {:id mask-id-top}
              [:path {:d mask-shape-top
                      :fill "#ffffff"}]])
           (when mask-shape-bottom
             [:mask {:id mask-id-bottom}
              [:path {:d mask-shape-bottom
                      :fill "#ffffff"}]])])
        (if (= alignment :even)
          [:<>
           (if (= mode :single)
             [render line-path nil (/ thickness-1 2)
              (tincture/pick tincture-2 render-options)
              outline? corner render-options]
             (cond
               (> thickness-1 thickness-2) [:<>
                                            [render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [render line-path nil (-> combined-thickness
                                                                      (/ 2)
                                                                      (- thickness-2))
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            [render line-path mask-id-bottom
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- outline/stroke-width))
                                             (tincture/pick tincture-1 render-options)
                                             false corner render-options]]
               (= thickness-1 thickness-2) [:<>
                                            [render line-path mask-id-top (/ combined-thickness 2)
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [render line-path mask-id-bottom (/ combined-thickness 2)
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            (when outline?
                                              [render line-path mask-id-bottom 0
                                               nil
                                               outline? corner render-options])]
               (< thickness-1 thickness-2) [:<>
                                            [render line-path nil (/ combined-thickness 2)
                                             (tincture/pick tincture-1 render-options)
                                             outline? corner render-options]
                                            [render line-path nil (-> combined-thickness
                                                                      (/ 2)
                                                                      (- thickness-1))
                                             (tincture/pick tincture-2 render-options)
                                             outline? corner render-options]
                                            [render line-path mask-id-top
                                             (cond-> (/ combined-thickness 2)
                                               outline? (- outline/stroke-width))
                                             (tincture/pick tincture-2 render-options)
                                             false corner render-options]]))]
          [:g {:mask (case alignment
                       :outside (str "url(#" mask-id-top ")")
                       :inside (str "url(#" mask-id-bottom ")")
                       nil)}
           (when (#{:double} mode)
             [render line-path nil combined-thickness
              (tincture/pick tincture-2 render-options)
              outline? corner render-options])
           (when (#{:single :double} mode)
             [render line-path nil thickness-1
              (tincture/pick tincture-1 render-options)
              outline? corner render-options])])])
     (when (and outline?
                (not (and (= alignment :even)
                          (#{:single :double} mode))))
       [:g outline/style
        [:path {:d line-path}]])]))
