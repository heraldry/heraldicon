(ns heraldicon.heraldry.ribbon
  (:require
   [heraldicon.context :as c]
   [heraldicon.font :as font]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.core :as math]
   [heraldicon.math.curve.bezier :as bezier]
   [heraldicon.math.curve.catmullrom :as catmullrom]
   [heraldicon.math.curve.core :as curve]
   [heraldicon.math.vector :as v]
   [heraldicon.options :as options]
   [heraldicon.svg.path :as path]))

(defn options [_context]
  {:thickness {:type :option.type/range
               :default 30
               :min 5
               :max 150
               :ui/label :string.option/thickness
               :ui/step 0.1}
   :edge-angle {:type :option.type/range
                :default 0
                :min -90
                :max 90
                :ui/label :string.option/edge-angle
                :ui/step 1
                :ui/tooltip :string.tooltip/edge-angle}
   :end-split {:type :option.type/range
               :default 0
               :min 0
               :max 80
               :ui/label :string.option/end-split
               :ui/step 1}
   :outline? {:type :option.type/boolean
              :default true
              :ui/label :string.charge.tincture-modifier.special/outline}})

(derive :heraldry/ribbon :heraldry.options/root)

(defmethod interface/options :heraldry/ribbon [context]
  (options context))

(defn- curve-segments [full-curve
                       last-index end-t last-edge-vector
                       index ts edge-vector]
  (assert (-> ts count (<= 2)) "curve-segments only supports 2 tangent points per segment")
  (let [full-curve (vec full-curve)
        first-leg (when-not (= last-index index 0)
                    (cond-> (get full-curve last-index)
                      end-t (->
                              (bezier/split end-t)
                              :bezier2)))]
    (if (empty? ts)
      [[(vec (concat (when first-leg [first-leg])
                     (subvec full-curve (inc last-index) (inc index))))
        last-edge-vector
        edge-vector]]
      (let [[t1 t2] ts
            first-split (-> full-curve
                            (get index)
                            (bezier/split t1))]
        (cond-> [[(vec (concat (when first-leg [first-leg])
                               (when (> index (inc last-index))
                                 (subvec full-curve (inc last-index) index))
                               [(:bezier1 first-split)]))
                  last-edge-vector
                  edge-vector]]
          t2 (conj [[(-> (:bezier2 first-split)
                         (bezier/split (/ (- t2 t1)
                                          (- 1 t1)))
                         :bezier1)]
                    edge-vector
                    edge-vector]))))))

(defn- split-curve [full-curve tangent-points min-edge-vector max-edge-vector]
  (if (empty? tangent-points)
    [[full-curve min-edge-vector max-edge-vector]]
    (->> (concat [[0 nil min-edge-vector]]
                 tangent-points
                 [[(-> full-curve count dec) nil max-edge-vector]])
         (partition 2 1)
         (mapcat (fn [[[last-index last-ts last-edge-vector]
                       [index ts edge-vector]]]
                   (curve-segments full-curve
                                   last-index (last last-ts) last-edge-vector
                                   index ts edge-vector)))
         vec)))

(defn generate-curves [points edge-angle]
  (let [curve (catmullrom/catmullrom points)
        num-legs (count curve)
        tangent-points (-> (keep-indexed
                            (fn [idx leg]
                              ;; TODO: probably better to calculate the angle based
                              ;; on the average x-value of the leg
                              (let [base-edge-vector (v/Vector. 0 1)
                                    leg-edge-angle (-> (* 2 edge-angle)
                                                       (/ (max 1 (dec num-legs)))
                                                       (* idx)
                                                       (- edge-angle))
                                    edge-vector (v/rotate base-edge-vector (- leg-edge-angle))
                                    ts (bezier/calculate-tangent-points leg edge-vector)]
                                (when (seq ts)
                                  [idx ts edge-vector])))
                            curve))
        curves-and-edge-vectors (split-curve curve tangent-points
                                             (v/rotate (v/Vector. 0 1)
                                                       edge-angle)
                                             (v/rotate (v/Vector. 0 1)
                                                       (- edge-angle)))]
    {:curve curve
     :curves (mapv first curves-and-edge-vectors)
     :edge-vectors (mapv (comp vec (partial drop 1)) curves-and-edge-vectors)}))

(def ^:private segment-type-choices
  [[:string.ribbon.segment-type-choice/foreground-with-text :heraldry.ribbon.segment.type/foreground-with-text]
   [:string.ribbon.segment-type-choice/foreground :heraldry.ribbon.segment.type/foreground]
   [:string.ribbon.segment-type-choice/background :heraldry.ribbon.segment.type/background]])

(derive :heraldry.ribbon.segment.type/foreground-with-text :heraldry.ribbon.segment/type)
(derive :heraldry.ribbon.segment.type/foreground :heraldry.ribbon.segment/type)
(derive :heraldry.ribbon.segment.type/background :heraldry.ribbon.segment/type)
(derive :heraldry.ribbon.segment/type :heraldry.ribbon/segment)

(def segment-type-map
  (options/choices->map segment-type-choices))

(def ^:private type-option
  {:type :option.type/choice
   :choices segment-type-choices
   :ui/label :string.option/type
   :ui/element :ui.element/radio-select})

(derive :heraldry.ribbon/segment :heraldry.options/root)

(defmethod interface/options :heraldry.ribbon/segment [context]
  (-> {:z-index {:type :option.type/range
                 :min 0
                 :max 100
                 :integer? true
                 :ui/label :string.option/layer}}
      (cond->
        (= (interface/get-raw-data (c/++ context :type))
           :heraldry.ribbon.segment.type/foreground-with-text)
        (merge {:offset-x {:type :option.type/range
                           :default 0
                           :min -0.5
                           :max 0.5
                           :ui/label :string.option/offset-x
                           :ui/step 0.01}
                :offset-y {:type :option.type/range
                           :default 0
                           :min -0.5
                           :max 0.5
                           :ui/label :string.option/offset-y
                           :ui/step 0.01}
                :font-scale {:type :option.type/range
                             :default 0.8
                             :min 0.01
                             :max 1
                             :ui/label :string.option/font-scale
                             :ui/step 0.01}
                :spacing {:type :option.type/range
                          :default 0.1
                          :min -0.5
                          :max 2
                          :ui/label :string.option/spacing
                          :ui/step 0.01}
                :text {:type :option.type/text
                       :default ""}
                :font (-> font/default-options
                          (assoc :default :berthold-baskerville))}))
      (assoc :type type-option)))

(defn project-path-to [curve new-start new-end & {:keys [reverse?]}]
  (let [original-start (ffirst curve)
        original-end (last (last curve))
        original-dir (if reverse?
                       (v/sub original-start original-end)
                       (v/sub original-end original-start))
        new-dir (if reverse?
                  (v/sub new-start new-end)
                  (v/sub new-end new-start))
        original-angle (-> (Math/atan2 (:y original-dir)
                                       (:x original-dir))
                           (* 180)
                           (/ Math/PI))
        new-angle (-> (Math/atan2 (:y new-dir)
                                  (:x new-dir))
                      (* 180)
                      (/ Math/PI))
        dist-original (v/abs original-dir)
        dist-new (v/abs new-dir)
        scale (if (math/close-to-zero? dist-original)
                1
                (/ dist-new
                   dist-original))
        angle (if (math/close-to-zero? dist-original)
                0
                (- new-angle
                   original-angle))]
    (-> curve
        path/curve-to-relative
        path/parse-path
        (cond->
          reverse? path/reverse)
        (path/scale scale scale)
        (path/rotate angle)
        (path/to-svg :relative? true))))

(defn project-bottom-edge [top-curve start-edge-vector end-edge-vector]
  (let [original-start (ffirst top-curve)
        original-end (last (last top-curve))
        new-start (v/add original-start start-edge-vector)
        new-end (v/add original-end end-edge-vector)]
    (project-path-to top-curve new-start new-end :reverse? true)))

(defn split-end [kind curve percentage edge-vector]
  (let [curve (vec curve)
        {:keys [curve1 curve2]} (curve/split curve (cond->> (/ percentage 100)
                                                     (= kind :end) (- 1)))
        split-point (v/add (ffirst curve2) (v/div edge-vector 2))]
    (case kind
      :start (let [start-point (ffirst curve)
                   end-point (v/add start-point edge-vector)]
               [(project-path-to curve1 end-point split-point)
                (project-path-to curve1 start-point split-point :reverse? true)])
      :end (let [start-point (last (last curve2))
                 end-point (v/add start-point edge-vector)]
             [(project-path-to curve2 split-point start-point :reverse? true)
              (project-path-to curve2 split-point end-point)]))))

(defmethod interface/properties :heraldry/ribbon [context]
  (let [points (interface/get-raw-data (c/++ context :points))
        thickness (interface/get-sanitized-data (c/++ context :thickness))
        bounding-box (when points
                       (bb/from-points (into points
                                             (map (partial v/add (v/Vector. 0 thickness)))
                                             points)))]
    {:type :heraldry/ribbon
     ;; TODO: not ideal, need the thickness here and need to know that the edge-vector (here
     ;; assumed to be (0, thickness) as a max) needs to be added to every point for the correct
     ;; height, the edge-angle also is ignored, which can affect the width
     :bounding-box bounding-box}))

(defmethod interface/bounding-box :heraldry/ribbon [context]
  (:bounding-box (interface/get-properties context)))
