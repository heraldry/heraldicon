(ns heraldicon.heraldry.helms
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]
   [heraldicon.math.vector :as v]))

(defmethod interface/properties :heraldry/helm [_context]
  {:type :heraldry/helm})

(defmethod interface/bounding-box :heraldry/helm [context]
  (let [components-context (c/++ context :components)
        num-components (interface/get-list-size components-context)]
    (->> (range num-components)
         (map #(c/++ components-context %))
         (map interface/get-bounding-box)
         (reduce bb/combine nil))))

(defmethod interface/environment :heraldry/helm [{:keys [path]
                                                  :as context}]
  (let [{:keys [environments]} (interface/get-properties (interface/parent context))]
    (get environments (last path))))

(defmethod interface/render-ir :heraldry/helm [context]
  (shield-separator/elements-ir (c/++ context :components)))

(defmethod interface/render-component :heraldry/helm [context]
  [interface/render-ir-root context])

(defmethod interface/properties :heraldry/helms [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)
        environments (when (pos? num-elements)
                       (let [{:keys [width]} (interface/get-parent-environment context)
                             gap-part-fn (fn [n] (+ 2 (* 2 (dec n))))
                             gap-part (gap-part-fn num-elements)
                             helm-width (/ (* gap-part width)
                                           (inc (* (inc gap-part)
                                                   num-elements)))
                             helm-height (* 2 helm-width)
                             gap (/ helm-width gap-part)
                             helm-width-with-gap (+ helm-width gap)]
                         (mapv (fn [idx]
                                 (environment/create (bb/from-vector-and-size
                                                      (v/Vector. (+ (* idx helm-width-with-gap) gap)
                                                                 (- helm-height))
                                                      helm-width
                                                      helm-height)))
                               (range num-elements))))]
    {:type :heraldry/helms
     :environments environments}))

(defmethod interface/bounding-box :heraldry/helms [context]
  (let [elements-context (c/++ context :elements)
        num-elements (interface/get-list-size elements-context)]
    (->> (range num-elements)
         (map #(c/++ elements-context %))
         (map interface/get-bounding-box)
         (reduce bb/combine nil))))

(defmethod interface/render-ir :heraldry/helms [context]
  (shield-separator/elements-ir (c/++ context :elements)))

(defmethod interface/render-component :heraldry/helms [context]
  [interface/render-ir-root context])
