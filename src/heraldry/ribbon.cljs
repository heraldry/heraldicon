(ns heraldry.ribbon
  (:require [heraldry.coat-of-arms.catmullrom :as catmullrom]
            [heraldry.coat-of-arms.vector :as v]
            [heraldry.interface :as interface]))

(def default-options
  {:thickness {:type :range
               :default 10
               :min 5
               :max 30
               :ui {:label "Thickness"
                    :step 0.1}}})

(defn options [data]
  default-options)

(defn curve-segments [full-curve
                      last-index end-t
                      index ts]
  (assert (-> ts count (<= 2)) "curve-segments only supports 2 tangent points per segment")
  (let [full-curve (vec full-curve)
        first-leg (when-not (= last-index index 0)
                    (cond-> (get full-curve last-index)
                      end-t (->
                             (catmullrom/split-bezier end-t)
                             :curve2)))]
    (if (empty? ts)
      [(-> (concat (when first-leg [first-leg])
                   (subvec full-curve (inc last-index) (inc index)))
           vec)]
      (let [[t1 t2] ts
            first-split (-> full-curve
                            (get index)
                            (catmullrom/split-bezier t1))]
        (cond-> [(-> (concat (when first-leg [first-leg])
                             (when (> index
                                      (inc last-index))
                               (subvec full-curve (inc last-index) index))
                             [(:curve1 first-split)])
                     vec)]
          t2 (conj [(-> (:curve2 first-split)
                        (catmullrom/split-bezier (/ (- t2 t1)
                                                    (- 1 t1)))
                        :curve1)]))))))

(defn split-curve [full-curve tangent-points]
  (if (empty? tangent-points)
    [full-curve]
    (->> (concat [[0 nil]]
                 tangent-points
                 [[(-> full-curve count dec) nil]])
         (partition 2 1)
         (mapcat (fn [[[last-index last-ts]
                       [index ts]]]
                   (curve-segments full-curve
                                   last-index (last last-ts)
                                   index ts)))
         vec)))

(defn generate-curves [points]
  (let [curve (catmullrom/catmullrom points)
        edge-vector (v/v 0 1)
        tangent-points (-> (keep-indexed
                            (fn [idx leg]
                              (let [ts (catmullrom/calculate-tangent-points leg ((juxt :x :y) edge-vector))]
                                (when (seq ts)
                                  [idx ts])))
                            curve))
        curves (split-curve curve tangent-points)]
    {:curve curve
     :curves curves}))

(def segment-options
  {:type {:type :choice
          :choices [["Foreground" :heraldry.ribbon.segment/foreground]
                    ["Background" :heraldry.ribbon.segment/background]]
          :ui {:label "Type"
               :form-type :radio-select}}
   :z-index {:type :range
             :min 0
             :max 100
             :integer? true
             :ui {:label "Layer"}}})

(defmethod interface/component-options :heraldry.component/ribbon-segment [_path _data]
  segment-options)

