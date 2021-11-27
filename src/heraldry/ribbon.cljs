(ns heraldry.ribbon
  (:require
   [heraldry.context :as c]
   [heraldry.font :as font]
   [heraldry.interface :as interface]
   [heraldry.math.bezier :as bezier]
   [heraldry.math.catmullrom :as catmullrom]
   [heraldry.math.core :as math]
   [heraldry.math.curve :as curve]
   [heraldry.math.svg.path :as path]
   [heraldry.math.vector :as v]
   [heraldry.strings :as strings]
   [heraldry.util :as util]))

(defn options [_context]
  {:thickness {:type :range
               :default 30
               :min 5
               :max 150
               :ui {:label strings/thickness
                    :step 0.1}}
   :edge-angle {:type :range
                :default 0
                :min -90
                :max 90
                :ui {:label {:en "Edge angle"
                             :de "Kantenwinkel"}
                     :step 1
                     :tooltip "This currently can cause glitches at some angles for some curves due to some numerical issues, set it carefully."}}
   :end-split {:type :range
               :default 0
               :min 0
               :max 80
               :ui {:label {:en "End split"
                            :de "Spaltung am Ende"}
                    :step 1}}
   :outline? {:type :boolean
              :default true
              :ui {:label strings/outline}}})

(defn curve-segments [full-curve
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
      [[(-> (concat (when first-leg [first-leg])
                    (subvec full-curve (inc last-index) (inc index)))
            vec)
        last-edge-vector
        edge-vector]]
      (let [[t1 t2] ts
            first-split (-> full-curve
                            (get index)
                            (bezier/split t1))]
        (cond-> [[(-> (concat (when first-leg [first-leg])
                              (when (> index
                                       (inc last-index))
                                (subvec full-curve (inc last-index) index))
                              [(:bezier1 first-split)])
                      vec)
                  last-edge-vector
                  edge-vector]]
          t2 (conj [[(-> (:bezier2 first-split)
                         (bezier/split (/ (- t2 t1)
                                          (- 1 t1)))
                         :bezier1)]
                    edge-vector
                    edge-vector]))))))

(defn split-curve [full-curve tangent-points min-edge-vector max-edge-vector]
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
                              (let [base-edge-vector (v/v 0 1)
                                    leg-edge-angle (-> (* 2 edge-angle)
                                                       (/ (max 1
                                                               (dec num-legs)))
                                                       (* idx)
                                                       (- edge-angle))
                                    edge-vector (-> base-edge-vector
                                                    (v/rotate (- leg-edge-angle)))
                                    ts (bezier/calculate-tangent-points leg edge-vector)]
                                (when (seq ts)
                                  [idx ts edge-vector])))
                            curve))
        curves-and-edge-vectors (split-curve curve tangent-points
                                             (-> (v/v 0 1)
                                                 (v/rotate edge-angle))
                                             (-> (v/v 0 1)
                                                 (v/rotate (- edge-angle))))]
    {:curve curve
     :curves (->> curves-and-edge-vectors
                  (map first)
                  vec)
     :edge-vectors (->> curves-and-edge-vectors
                        (map (comp vec (partial drop 1)))
                        vec)}))

(def segment-type-choices
  [[{:en "Text"
     :de "Text"} :heraldry.ribbon.segment/foreground-with-text]
   [{:en "Foreground"
     :de "Vorderseite"} :heraldry.ribbon.segment/foreground]
   [{:en "Background"
     :de "Rückseite"} :heraldry.ribbon.segment/background]])

(def segment-type-map
  (util/choices->map segment-type-choices))

(def type-option
  {:type :choice
   :choices segment-type-choices
   :ui {:label strings/type
        :form-type :radio-select}})

(defmethod interface/options-subscriptions :heraldry.component/ribbon-segment [_context]
  #{[:type]})

(defmethod interface/options :heraldry.component/ribbon-segment [context]
  (-> {:z-index {:type :range
                 :min 0
                 :max 100
                 :integer? true
                 :ui {:label strings/layer}}}
      (cond->
        (= (interface/get-raw-data (c/++ context :type))
           :heraldry.ribbon.segment/foreground-with-text)
        (merge {
                :offset-x {:type :range
                           :default 0
                           :min -0.5
                           :max 0.5
                           :ui {:label strings/offset-x
                                :step 0.01}}
                :offset-y {:type :range
                           :default 0
                           :min -0.5
                           :max 0.5
                           :ui {:label strings/offset-y
                                :step 0.01}}
                :font-scale {:type :range
                             :default 0.8
                             :min 0.01
                             :max 1
                             :ui {:label {:en "Font scale"
                                          :de "Schrift-Faktor"}
                                  :step 0.01}}
                :spacing {:type :range
                          :default 0.1
                          :min -0.5
                          :max 2
                          :ui {:label strings/spacing
                               :step 0.01}}
                :text {:type :text
                       :default ""}
                :font (-> font/default-options
                          (assoc :default :baskerville-berthold))}))
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
        {:keys [curve1 curve2]} (curve/split curve (cond-> (/ percentage 100)
                                                     (= kind :end) (->> (- 1))))
        split-point (-> (ffirst curve2)
                        (v/add (v/div edge-vector 2)))]
    (case kind
      :start (let [start-point (ffirst curve)
                   end-point (v/add start-point edge-vector)]
               [(project-path-to curve1 end-point split-point)
                (project-path-to curve1 start-point split-point :reverse? true)])
      :end (let [start-point (last (last curve2))
                 end-point (v/add start-point edge-vector)]
             [(project-path-to curve2 split-point start-point :reverse? true)
              (project-path-to curve2 split-point end-point)]))))
