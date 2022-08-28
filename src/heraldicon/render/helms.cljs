(ns heraldicon.render.helms
  (:require
   [heraldicon.context :as c]
   [heraldicon.heraldry.field.environment :as environment]
   [heraldicon.heraldry.shield-separator :as shield-separator]
   [heraldicon.interface :as interface]
   [heraldicon.math.bounding-box :as bb]))

(defn helm [context & {:keys [below-shield?]}]
  (into [:<>]
        (map (fn [[idx self-below-shield?]]
               ^{:key idx}
               [interface/render-component
                (-> context
                    (c/++ :components idx)
                    (assoc :auto-resize? false
                           :self-below-shield? self-below-shield?
                           :render-pass-below-shield? below-shield?))])
             (shield-separator/get-element-indices (c/++ context :components)))))

(defn render [context width]
  (let [num-helms (interface/get-list-size (c/++ context :elements))]
    (if (zero? num-helms)
      {:width 0
       :height 0
       :result nil}
      (let [gap-part-fn (fn [n] (+ 2 (* 2 (dec n))))
            gap-part (gap-part-fn num-helms)
            helm-width (/ (* gap-part width)
                          (inc (* (inc gap-part)
                                  num-helms)))
            helm-height (* 2 helm-width)
            total-height helm-height
            gap (/ helm-width gap-part)
            helm-width-with-gap (+ helm-width gap)]
        {:width (- width
                   (* 2 gap))
         :height total-height
         :result-below-shield (into [:g]
                                    (map (fn [idx]
                                           (let [helm-environment (environment/create
                                                                   nil
                                                                   {:bounding-box (bb/BoundingBox.
                                                                                   (+ (* idx helm-width-with-gap)
                                                                                      gap)
                                                                                   (+ (* idx helm-width-with-gap)
                                                                                      gap
                                                                                      helm-width)
                                                                                   (- helm-height)
                                                                                   0)})]
                                             ^{:key idx}
                                             [helm (-> context
                                                       (c/++ :elements idx)
                                                       (assoc :environment helm-environment))
                                              :below-shield? true])))
                                    (range num-helms))
         :result-above-shield (into [:g]
                                    (map (fn [idx]
                                           (let [helm-environment (environment/create
                                                                   nil
                                                                   {:bounding-box (bb/BoundingBox.
                                                                                   (+ (* idx helm-width-with-gap)
                                                                                      gap)
                                                                                   (+ (* idx helm-width-with-gap)
                                                                                      gap
                                                                                      helm-width)
                                                                                   (- helm-height)
                                                                                   0)})]
                                             ^{:key idx}
                                             [helm (-> context
                                                       (c/++ :elements idx)
                                                       (assoc :environment helm-environment))])))
                                    (range num-helms))}))))
