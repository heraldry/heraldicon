(ns heraldicon.heraldry.field.type.plumetty
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.interface :as field.interface]
   [heraldicon.heraldry.tincture :as tincture]
   [heraldicon.interface :as interface]
   [heraldicon.math.vector :as v]
   [heraldicon.render.outline :as outline]
   [heraldicon.util.uid :as uid]))

(def field-type :heraldry.field.type/plumetty)

(defmethod field.interface/display-name field-type [_] :string.field.type/plumetty)

(defmethod field.interface/part-names field-type [_] nil)

(defmethod field.interface/options field-type [_context]
  {:thickness {:type :option.type/range
               :min 0
               :max 1
               :default 0.5
               :ui/label :string.option/thickness
               :ui/step 0.01}
   :layout {:num-fields-x {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :ui/label :string.option/subfields-x
                           :ui/element :ui.element/field-layout-num-fields-x}
            :num-fields-y {:type :option.type/range
                           :min 1
                           :max 20
                           :default 6
                           :default-display-value "auto"
                           :ui/label :string.option/subfields-y
                           :ui/element :ui.element/field-layout-num-fields-y}
            :offset-x {:type :option.type/range
                       :min -1
                       :max 1
                       :default 0
                       :ui/label :string.option/offset-x
                       :ui/step 0.01}
            :offset-y {:type :option.type/range
                       :min -1
                       :max 1
                       :default 0
                       :ui/label :string.option/offset-y
                       :ui/step 0.01}
            :stretch-x {:type :option.type/range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui/label :string.option/stretch-x
                        :ui/step 0.01}
            :stretch-y {:type :option.type/range
                        :min 0.5
                        :max 2
                        :default 1
                        :ui/label :string.option/stretch-y
                        :ui/step 0.01}
            :rotation {:type :option.type/range
                       :min -180
                       :max 180
                       :default 0
                       :ui/label :string.option/rotation
                       :ui/step 0.01}
            :ui/label :string.option/layout
            :ui/element :ui.element/field-layout}})

(defn- feather-vein-paths
  "Generate vein paths for a single feather. cx is the center x, hat-tip-y is
   the pointed top of the hat (one row above the body), full-depth is the total
   feather extent (hat + body = 2 * part-height)."
  [cx hat-tip-y full-depth half-width stroke-width]
  (let [;; start the spine below the hat tip so the round cap doesn't poke above
        spine-start-y (+ hat-tip-y (/ stroke-width 2))
        spine-length (- (* full-depth 0.66) (/ stroke-width 2))
        spine-end-y (+ spine-start-y spine-length)
        num-barbs 4
        ;; slight curve on the spine
        spine (str "M" cx "," spine-start-y
                   "Q" (+ cx (* half-width 0.02)) "," (+ spine-start-y (* spine-length 0.5))
                   " " cx "," spine-end-y)]
    (into [spine]
          (mapcat
           (fn [i]
             (let [t (/ i (+ num-barbs 0.5))
                   sy (+ hat-tip-y (* t spine-length))
                   ;; barbs narrow toward the tip
                   spread (* half-width 0.45 (- 1 (* t 0.5)))
                   ;; barb endpoints curve slightly downward
                   by (+ sy (* spine-length 0.06))
                   ;; control point: slightly off-center for gentle curve
                   qx-offset (* spread 0.5)
                   qy (- sy (* spine-length 0.02))]
               [(str "M" cx "," sy
                     "Q" (- cx qx-offset) "," qy
                     " " (- cx spread) "," by)
                (str "M" cx "," sy
                     "Q" (+ cx qx-offset) "," qy
                     " " (+ cx spread) "," by)])))
          (range 1 (inc num-barbs)))))

(defn- vein-group
  [paths stroke-width]
  (into [:g {:fill "none"
             :stroke-width stroke-width
             :stroke-linecap "round"}]
        (map (fn [d] [:path {:d d}]))
        paths))

(defn- plumetty-default [part-width part-height thickness]
  (let [width part-width
        height (* 4 part-height)
        mx (/ width 2)
        ph part-height
        ph2 (* 2 ph)
        ph3 (* 3 ph)
        ph4 (* 4 ph)
        cx-right (* width 0.75)
        cx-left (* width 0.25)
        cy-frac 0.84
        vein-stroke-width (* thickness 0.05 width)]
    {:width width
     :height height

     :pattern [:<>
               [:path {:d (str "M 0,0"
                               "h" width
                               "Q" cx-right "," (* ph cy-frac) " " mx "," ph
                               "Q" cx-left "," (* ph cy-frac) " 0,0"
                               "z")}]
               [:path {:d (str "M 0," ph2
                               "h" width
                               "Q" cx-right "," (+ ph2 (* ph cy-frac)) " " mx "," ph3
                               "Q" cx-left "," (+ ph2 (* ph cy-frac)) " 0," ph2
                               "z")}]
               [:path {:d (str "M 0," ph2
                               "Q" cx-left "," (- ph2 (* ph (- 1 cy-frac))) " " mx "," ph
                               "Q" cx-right "," (- ph2 (* ph (- 1 cy-frac))) " " width "," ph2
                               "z")}]
               [:path {:d (str "M 0," ph4
                               "Q" cx-left "," (- ph4 (* ph (- 1 cy-frac))) " " mx "," ph3
                               "Q" cx-right "," (- ph4 (* ph (- 1 cy-frac))) " " width "," ph4
                               "z")}]]

     :outline (let [;; offset feather control points: feather at (-mx, y) has
                    ;; right-half control at (mx*0.5, y+ph*cy-frac)
                    ;; left-half control at (-mx*0.5, y+ph*cy-frac) — outside tile, wraps
                    ;; feather at (mx, y) has
                    ;; right-half control at (mx*1.5, y+ph*cy-frac)
                    ;; left-half control at (mx*0.5, y+ph*cy-frac)
                    ocx-inner (* mx 0.5)
                    ocx-outer (* mx 1.5)]
                [:<>
                 ;; row 0 curves
                 [:path {:d (str "M" width ",0"
                                 "Q" cx-right "," (* ph cy-frac) " " mx "," ph
                                 "Q" cx-left "," (* ph cy-frac) " 0,0")}]
                 ;; row 1 curves (offset): from (mx,ph) down to (0,ph2) and (width,ph2)
                 [:path {:d (str "M" mx "," ph
                                 "Q" ocx-inner "," (+ ph (* ph cy-frac)) " 0," ph2)}]
                 [:path {:d (str "M" mx "," ph
                                 "Q" ocx-outer "," (+ ph (* ph cy-frac)) " " width "," ph2)}]
                 ;; row 2 curves
                 [:path {:d (str "M" width "," ph2
                                 "Q" cx-right "," (+ ph2 (* ph cy-frac)) " " mx "," ph3
                                 "Q" cx-left "," (+ ph2 (* ph cy-frac)) " 0," ph2)}]
                 ;; row 3 curves (offset): from (mx,ph3) down to (0,ph4) and (width,ph4)
                 [:path {:d (str "M" mx "," ph3
                                 "Q" ocx-inner "," (+ ph3 (* ph cy-frac)) " 0," ph4)}]
                 [:path {:d (str "M" mx "," ph3
                                 "Q" ocx-outer "," (+ ph3 (* ph cy-frac)) " " width "," ph4)}]])

     ;; veins on even-row feathers (rows 0, 2)
     ;; hat tip is one row above the body: row-0 hat tip at -ph,
     ;; row-2 hat tip at ph. Full feather depth = 2*ph.
     ;; Row 0 needs a wrapped copy at +height so the bottom hat gets veins.
     :veins-even (let [hw (* mx 0.7)
                       fd (* 2 ph)
                       sw vein-stroke-width]
                   [:<>
                    (vein-group (feather-vein-paths mx (- ph) fd hw sw) sw)
                    (vein-group (feather-vein-paths mx (+ (- ph) ph4) fd hw sw) sw)
                    (vein-group (feather-vein-paths mx ph fd hw sw) sw)])

     ;; veins on odd-row feathers (rows 1, 3) — at tile edges
     ;; row-1 hat tip at 0, row-3 hat tip at ph2
     :veins-odd (let [hw (* mx 0.7)
                      fd (* 2 ph)
                      sw vein-stroke-width]
                  [:<>
                   (vein-group (feather-vein-paths 0 0 fd hw sw) sw)
                   (vein-group (feather-vein-paths width 0 fd hw sw) sw)
                   (vein-group (feather-vein-paths 0 ph2 fd hw sw) sw)
                   (vein-group (feather-vein-paths width ph2 fd hw sw) sw)])}))

(defn- render [context {:keys [start part-width part-height rotation thickness]}]
  (let [outline? (or (interface/render-option :outline? context)
                     (interface/get-sanitized-data (c/++ context :outline?)))
        veins? (pos? thickness)
        pattern-id-prefix (uid/generate "plumetty")
        {pattern-width :width
         pattern-height :height
         plumetty-pattern :pattern
         plumetty-outline :outline
         veins-even :veins-even
         veins-odd :veins-odd} (plumetty-default part-width part-height thickness)
        pattern-props {:width pattern-width
                       :height pattern-height
                       :x (:x start)
                       :y (:y start)
                       :pattern-units "userSpaceOnUse"}
        mask-even-id (when veins? (uid/generate "mask"))
        mask-odd-id (when veins? (uid/generate "mask"))]
    [:g {:transform (str "rotate(" (- rotation) ")")}
     [:defs
      (when outline?
        [:pattern (assoc pattern-props :id (str pattern-id-prefix "-outline"))
         [:g (outline/style context)
          plumetty-outline]])
      (into [:<>]
            (map (fn [idx]
                   ^{:key idx}
                   [:pattern (assoc pattern-props :id (str pattern-id-prefix "-" idx))
                    [:rect {:x -1
                            :y -1
                            :width (+ pattern-width 2)
                            :height (+ pattern-height 2)
                            :shape-rendering "crispEdges"
                            :fill (get ["#ffffff" "#000000"] idx)}]
                    [:g {:fill (get ["#000000" "#ffffff"] idx)}
                     plumetty-pattern]]))
            (range 2))
      (when veins?
        (let [tincture-0-colour (tincture/pick
                                 (interface/get-sanitized-data
                                  (c/++ context :fields 0 :field :tincture))
                                 context)
              tincture-1-colour (tincture/pick
                                 (interface/get-sanitized-data
                                  (c/++ context :fields 1 :field :tincture))
                                 context)]
          [:<>
           [:pattern (assoc pattern-props :id (str pattern-id-prefix "-veins-even"))
            [:g {:stroke tincture-0-colour}
             veins-even]]
           [:pattern (assoc pattern-props :id (str pattern-id-prefix "-veins-odd"))
            [:g {:stroke tincture-1-colour}
             veins-odd]]
           (when outline?
             [:<>
              [:pattern (assoc pattern-props :id (str pattern-id-prefix "-veins-even-outline"))
               [:g (merge (outline/style context)
                          {:stroke-width (* outline/stroke-width 0.6)})
                veins-even]]
              [:pattern (assoc pattern-props :id (str pattern-id-prefix "-veins-odd-outline"))
               [:g (merge (outline/style context)
                          {:stroke-width (* outline/stroke-width 0.6)})
                veins-odd]]])]))]
     ;; tincture fills
     (into [:<>]
           (map (fn [idx]
                  (let [mask-id (uid/generate "mask")]
                    ^{:key idx}
                    [:<>
                     [:mask {:id mask-id}
                      [:rect {:x -500
                              :y -500
                              :width 1100
                              :height 1100
                              :fill (str "url(#" pattern-id-prefix "-" idx ")")}]]
                     [tincture/tinctured-field
                      (c/++ context :fields idx :field)
                      :mask-id mask-id]])))
           (range 2))
     (when veins?
       [:<>
        [:mask {:id mask-even-id}
         [:rect {:x -500 :y -500 :width 1100 :height 1100
                 :fill (str "url(#" pattern-id-prefix "-1)")}]]
        [:rect {:x -500 :y -500 :width 1100 :height 1100
                :fill (str "url(#" pattern-id-prefix "-veins-even)")
                :mask (str "url(#" mask-even-id ")")}]
        [:mask {:id mask-odd-id}
         [:rect {:x -500 :y -500 :width 1100 :height 1100
                 :fill (str "url(#" pattern-id-prefix "-0)")}]]
        [:rect {:x -500 :y -500 :width 1100 :height 1100
                :fill (str "url(#" pattern-id-prefix "-veins-odd)")
                :mask (str "url(#" mask-odd-id ")")}]])
     (when outline?
       [:<>
        (when veins?
          [:<>
           [:rect {:x -500 :y -500 :width 1100 :height 1100
                   :fill (str "url(#" pattern-id-prefix "-veins-even-outline)")
                   :mask (str "url(#" mask-even-id ")")}]
           [:rect {:x -500 :y -500 :width 1100 :height 1100
                   :fill (str "url(#" pattern-id-prefix "-veins-odd-outline)")
                   :mask (str "url(#" mask-odd-id ")")}]])
        [:rect {:x -500
                :y -500
                :width 1100
                :height 1100
                :fill (str "url(#" pattern-id-prefix "-outline)")}]])]))

(defmethod interface/properties field-type [context]
  (let [{:keys [width height points]} (interface/get-subfields-environment context)
        {:keys [top-left]} points
        num-fields-x (interface/get-sanitized-data (c/++ context :layout :num-fields-x))
        num-fields-y (interface/get-sanitized-data (c/++ context :layout :num-fields-y))
        raw-num-fields-y (interface/get-raw-data (c/++ context :layout :num-fields-y))
        offset-x (interface/get-sanitized-data (c/++ context :layout :offset-x))
        offset-y (interface/get-sanitized-data (c/++ context :layout :offset-y))
        stretch-x (interface/get-sanitized-data (c/++ context :layout :stretch-x))
        stretch-y (interface/get-sanitized-data (c/++ context :layout :stretch-y))
        rotation (interface/get-sanitized-data (c/++ context :layout :rotation))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        part-width (-> width
                       (/ num-fields-x)
                       (* stretch-x))
        unstretched-part-height (if raw-num-fields-y
                                  (/ height num-fields-y)
                                  (/ part-width 2))
        part-height (* unstretched-part-height stretch-y)
        middle-x (/ width 2)
        middle-y (/ height 2)
        shift-x (- middle-x
                   (* middle-x stretch-x))
        shift-y (- middle-y
                   (* middle-y stretch-y))
        x0 (+ (:x top-left)
              (* part-width offset-x)
              shift-x)
        y0 (+ (:y top-left)
              (* part-height offset-y)
              shift-y)
        start (v/Vector. x0 y0)]
    {:type field-type
     :start start
     :num-fields-x num-fields-x
     :num-fields-y num-fields-y
     :part-width part-width
     :part-height part-height
     :rotation rotation
     :thickness thickness
     :render-fn render}))

(defmethod interface/subfield-environments field-type [_context]
  nil)

(defmethod interface/subfield-render-shapes field-type [_context]
  nil)
